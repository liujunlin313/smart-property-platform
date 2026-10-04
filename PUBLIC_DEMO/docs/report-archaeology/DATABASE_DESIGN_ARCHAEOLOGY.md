# 数据库设计考古：关系、约束、版本与数据链

> 事实级别：A=当前源码/迁移直接证实；B=2026-09-01 SQL 备份证实；C=历史测试/审计报告记载。本轮没有访问或改写在线业务库。凡称“现有表结构”均是 A/B 交叉结果；历史测试数值不冒充当日现测。

## 1. 概念模型与关系分解

空间主键链是 `community.community_id → building.community_id → building_unit.building_id → property_unit.building_unit_id`（`V1__init_schema.sql:7–105`）。每级有自己的 PK 和兄弟层级唯一键，如 `uk_building_community_no(community_id,building_no)`、`uk_building_unit_building_no(building_id,unit_no)`、`uk_property_unit_parent_no(building_unit_id,property_no)`，避免房号脱离所属单元被全局错误去重。`layout_template` 是可复用的户型设计，不等同于某一套实际 `property_unit`；后续 `layout_template_floor_plan` 和 `layout_template_floor_plan_variant` 使模板图可按面积变体复用，实际房产上传则走 `property_floor_plan`。

`person` 不是“业主表”。`ownership(person_id,property_unit_id,start_date,end_date,ownership_share)` 表示产权时段，`residence(person_id,property_unit_id,start_date,end_date,residence_role,relation_to_responsible,...)` 表示入住时段，两者独立；同一人可拥有多房却住在另一房，A05/A06 种子断言专门验证（`database/tests/05_seed_assertions.sql`）。当前关系看 `end_date IS NULL`，历史关系仍留行。V3 给 `ownership` 加生成列 `current_property_unit_id` 与唯一键 `uk_ownership_one_current_owner`，将“同房只有一个当前业主”的当前版本规则下沉到数据库；原 V1 对 `ownership_share` 的开放份额校验被改为 `=1.000000`，这是模型演进，不能仍按 V1 讲多人份额。现行 `PropertyMapper.xml` 将 `residence_role='RESPONSIBLE'` 结合占用状态映射为户主/主租户，其他成员用 `relation_to_responsible` 区分配偶、父母、子女、合租人。

`authorization` 是 Person–PropertyUnit–Agency 的三元关系，不可简单存为“房产属于中介”；`rental_listing` 的 `(authorization_id,property_unit_id)` 复合外键保证代理挂牌的授权确实属于该房产，NEG-15 验证错配被拒。`lease_contract.rental_listing_id` 唯一键使单挂牌至多形成一份合同，`contract_participation` 将合同与 Person 的房东/租客 N:M 关系实体化，NEG-16 检验重复合同。租赁表、车证表、通知收件表都是“看似多一张、实际保存独立业务事实”的合适课堂案例。

账务分层是 `fee_type`（收费种类）→ `fee_standard`（小区/类型/生效日费率版本）→ `bill`（房产×年月的账单头）→ `bill_item`（明细，PK=`bill_id,item_no`）以及 `payment`（付款事实，外键回账单）。表计 `meter` 与 `meter_reading` 另存设备和读数；计量明细可关联前后读数。`bill` 的 `uk_bill_property_period` 阻止同一房产同账期重复账单，`payment` 的业务单号/外部单号有唯一约束。`parking_right` 的主体是 Person 与 ParkingSpace，并非 Vehicle；车辆登记、车位长期权利、居民车证资格、申请和最终证照分表，业务语义不同。`notification` 与 `person` 的 N:M 由 `notification_recipient` 实体化，`recipient_role`、`delivery_status`、`read_at` 属于每一条具体收件关系（`V4`）。

ER 可从 `docs/ppt_assets/global_er_concept.html` 的概念主图开始，再按 `docs/ppt-er/01_global_core_er.html` 至 `06_resident_service_notification_er.html` 展开；概念图不能误当物理表 DDL。V1 设施模型以 `facility_placement` 在整套房二维比例坐标中放置家具/设备，A12–A14 证实没有独立 Room 表。

## 2. V1–V10 的真实演进

| 版本/文件 | 增量事实 | 解决的问题/注意 |
|---|---|---|
| V1 `V1__init_schema.sql` | 25 张核心表；空间、产权/入住、中介授权/挂牌/合同、收费/表计、车位、设施；大量 PK/FK/UK/CHECK | 初始完整关系模型，不是当前 46 张表的全部 |
| V2 `V2__seed_reference_data.sql` | `layout_template`、`fee_type`、`facility_type` 参考数据 | 提供字典/模板；不是建表版 |
| V3 `V3__phase2a_security_assets_and_files.sql` | 新增 8 表：`app_account`、`app_role`、`account_global_role`、`account_agency_role`、`refresh_token_session`、`file_object`、`property_floor_plan`、`asset_change_audit`；房产/楼栋版本列、位置字段、当前产权唯一规则 | 从课程业务模型进入身份、乐观锁、对象文件与审计；改变了早期产权份额约束 |
| V4 `V4__stage_f_access_scope_boundary_notification.sql` | 新增 5 表：`app_permission`、`role_permission`、`account_community_scope`、`spatial_boundary`、`notification_recipient`；扩展既有 `notification` 的发送人、状态、时间与类别 | 角色权限、多小区数据作用域、空间边界、具体收件关系 |
| V5 `V5__notification_recipient_charge_payload.sql` | 给 `notification_recipient` 增收件人级收费载荷字段 | 收费提醒按收件关系保留结构化上下文；无新表 |
| V6 `V6__stage_g_bill_period_status_index.sql` | 新增 `idx_bill_period_status(billing_year,billing_month,status,bill_id)` | 账期/状态筛选；历史 `STAGE_G_INDEX_AUDIT.md` 记载筛选约 603→11 行的计划对照，不代表普适速度增益 |
| V7 `V7__stage_h_residence_meter_and_incident.sql` | `residence.expected_end_date` 与日期 CHECK；新增 `resident_incident`；补权限 | 租住预期终期和居民报事 |
| V8 `V8__parking_permit_management.sql` | 新增 `residence_parking_eligibility`、`parking_permit_application`、`parking_permit`，多项价额/状态/有效期 CHECK；并含演示记录 INSERT | 资格→申请→审核发证的分层，不是简单车位 CRUD；此 migration 混入业务演示数据，正式报告须诚实说明 |
| V9 `V9__layout_template_floor_plans.sql` | 新增 `layout_template_floor_plan`、文件关联唯一键与 FK | 模板标准图 |
| V10 `V10__layout_template_floor_plan_variants.sql` | 新增 `layout_template_floor_plan_variant`、`(layout_template_id,gross_area)` 唯一键及文件 FK | 同模板按面积匹配变体图 |

V1 25 + V3 8 + V4 5 + V7 1 + V8 3 + V9 1 + V10 1 = **44 张由这十版明确建出的表**。备份另有 `agency_staff`、`notification` 两张业务表和 `flyway_schema_history` 一张元数据表，共 47。`agency_staff`、`notification` 原始建表脚本、两个 VIEW 原始建视图脚本未在现存 V1–V10 找到；备份保留其最终定义。`V4` 注释也明言 `notification` 为 Pre-Stage1 已有表。故 Flyway 提供版本记录和多数结构演进，但不能宣称所有结构均以现存迁移为唯一来源。`docs/DOCKER_STAGE1_RUNBOOK.md` 记录恢复库中 V1–V10 共 10 条成功历史；当前 Docker 关闭自动迁移以保护已恢复数据。

## 3. 约束为何不是装饰

| 现实规则 | DDL/非法例 | 已有证据 |
|---|---|---|
| 楼号、单元号、房号在其父级内唯一 | `uk_building_community_no`、`uk_building_unit_building_no`、`uk_property_unit_parent_no`；重复插入报 1062 | `03_constraint_negative_cases.sql` NEG-01～03，`evidence/07_negative_cases.txt` |
| 不能引用不存在的小区/楼/单元 | `fk_building_community`、`fk_building_unit_building`、`fk_property_unit_building_unit` 等，`ON DELETE RESTRICT`；报 1452 | NEG-04～06 |
| 产权/入住时段合法 | `ck_ownership_dates`、`ck_residence_dates`；结束早于开始报 3819 | NEG-08～09；V3 又收紧当前业主唯一性 |
| 授权必须有存在的人、合法期限和房产一致性 | `authorization` FK/CHECK；挂牌复合 FK | NEG-10、11、15 |
| 账单项须属于真实账单 | `bill_item.bill_id` FK，`bill_item` PK=(bill_id,item_no)；孤立账单行报 1452 | NEG-12；A09/A10 证实明细与读数示例 |
| 单挂牌至多一合同 | `uk_lease_contract_listing`；重复报 1062 | NEG-16 |
| 设施位置/移动模式组合合法 | `ck_facility_mobility`；LIMITED 无锚点/半径报 3819 | NEG-14 |
| 收件人不重复、发证不重复 | `uk_notification_recipient_once(notification_id,person_id)`、`uk_parking_permit_application` 等 | V4/V8 DDL；本轮未额外跑对应负例 |
| 资格码、车证状态/价额/日期限定 | V8 `ck_residence_parking_eligibility_reason` 与申请/车证 CHECK | 2026-08-30 历史真实失败与事务回滚见 `docs/PPT_TRANSACTION_ROLLBACK_EVIDENCE.md`；不能把独立的“新增人员”请求也说成被回滚 |

NEG-01～16 是 **16 条预期失败 SQL**，`evidence/VERIFICATION_SUMMARY.txt` 在 2026-08-21 的隔离 V1 验证记录“16 expected failures observed”；A01～14 有 **14 个 PASS**。它们不能代表 V3–V10 所有新约束都已有同等级负向回归。备份库的 Stage1 Runbook 另记录 77 FK、108 CHECK、`information_schema.statistics` 的 208 行 index entries；后者是统计行，不是 208 个独立索引对象。

## 4. 多小区不是简单的多行 `community`

真实路径：Spring Security 验 JWT → `SpringSecurityActorContextProvider` 生成 `ActorContext(accountId,personId,roles,...)` → `AuthorizationServiceImpl.requirePermission` 用 `AccessControlMapper.selectPermissionCodes` 查询 `account_global_role JOIN role_permission JOIN app_permission` → `DataScopeServiceImpl.currentCommunityScope` 判断 `SYSTEM_ADMIN` 全局，否则查询 `account_community_scope` 中 `status='ACTIVE'` 的 community ID → 服务将 `CommunityDataScope` 或 `CommunityScopeQuery` 交给 Mapper → MyBatis XML 在 `building.community_id`/`community.community_id` 加 `IN (<foreach>#{scopeId}</foreach>)`。若没有授权小区，`<otherwise>AND 1=0`，不是省略过滤条件（源码：`backend/src/main/java/com/smartproperty/platform/access/service/{AuthorizationServiceImpl,DataScopeServiceImpl}.java`、`backend/src/main/resources/mapper/access/AccessControlMapper.xml`、`mapper/stage1/PropertyMapper.xml` 等）。

历史 `docs/STAGE_G_IDENTITY_PROVISIONING_REPORT.md` 记录：`staff_scope_a` 管社区 4；`staff_scope_b` 管 5；`staff_scope_c` 管 4 与 6；`staff_scope_d` 管 7；`admin` 是全局。2026-09-01 备份中这四个用户名对应 `app_account.account_id` 分别为 **25、26、27、28**；同一备份的 `account_community_scope` 数据行为 `(25,4)`、`(26,5)`、`(27,4)`、`(27,6)`、`(28,7)`，状态均为 `ACTIVE`。仅提取 ID 与用户名/社区对照，未展示密码散列。`docs/STAGE_H_R1_IDENTITY_SCOPE_SECURITY_REPAIR_REPORT.md` 实测旧房产分页 a=72、c=168，Dashboard 房产数也为 72/168，居民直接读旧 `/api/v1/properties` 返回 403。这是“同一数据库、不同账号看到不同社区集合”的证据，而非按用户名写死分支。特定房产/账单/入住/楼栋写入还用 `requireProperty/requireBill/requireResidence/requireBuilding` 回查所属小区；读路径的 SQL 过滤和对象级写权限校验互补。

## 5. 账务 VIEW：从真实备份恢复的最终定义

两 VIEW 的**最终**定义位于 `deployment_backups/community_db_before_public_demo_20260901.sql` 尾部 `Final view structure`。文件中部还有同名 *Temporary view structure*，其中字段都写 `1 AS ...`，只是 `mysqldump` 的依赖占位，绝不能当成业务公式。

`v_bill_financial_summary` 从 `bill b` 出发，`LEFT JOIN` 按 `bill_id` 聚合的 `bill_item` 子查询 `i`，得到应收 `SUM(amount)`；再 `LEFT JOIN` 按 `bill_id` 聚合且 `payment.status='SUCCESS'` 的付款子查询 `p`，得到实收。结果字段为 `bill_id/bill_no/property_unit_id/billing_year/billing_month/issued_at/due_date/status` 加 `ROUND(COALESCE(i.receivable_amount,0),2)`、`ROUND(COALESCE(p.received_amount,0),2)`、`ROUND(GREATEST(COALESCE(i.receivable_amount,0)-COALESCE(p.received_amount,0),0),2)`。LEFT JOIN 保留尚无账单行或成功付款的账单；COALESCE 将不存在的聚合映为金额 0；GREATEST 不让欠费显示负值；ROUND 固定两位货币精度。这里“没有付款视为实收 0”是**已存在账单**的账务口径，和“某月根本无账单”不同。

`v_monthly_collection_summary` 在前一 VIEW 上按 `billing_year,billing_month` 分组，汇总账单数、应收/实收/欠费与 `SUM(received)/NULLIF(SUM(receivable),0)*100` 收缴率。当前**受 DataScope 约束的** `StatisticsMapper.xml:6–16` 为避免全局聚合 VIEW 无法按小区重新分割，主要从 `v_bill_financial_summary` 联房产→楼栋→小区后再分组；不能声称 Dashboard 现在直接查月度全局 VIEW。年度图完整 12 月的无账单月份应以 null/no-data 表达，不能拿 0 元伪造出账（见年度统计页面和历史 Stage C 约定）。

## 6. MyBatis 与 8 条可追踪业务链

| 链路 | 页面/API→Controller→Service→Mapper XML→SQL/VO | 数据库要点 |
|---|---|
| 旧资产房产分页 | `frontend/src/views/AssetsView.vue` → `frontend/src/api/assets.js getProperties` → `/api/v1/properties` → `asset/controller/PropertyUnitController.findAll` → `asset/service/PropertyUnitService.findAll` → `asset/mapper/PropertyUnitMapper.{count,findAll}` → `mapper/asset/PropertyUnitMapper.xml` → `PropertyUnitResponse`/`PagedResponse` | 先 `COUNT(*)` 同条件取 total；`LIMIT #{limit} OFFSET #{offset}`，offset=`(page-1)*pageSize`；白名单排序由 `<choose>/<when>` 输出列名而非拼接用户 SQL；`PagedResponse.of` 计算 `ceil(total/pageSize)` |
| 管理端房产页 | `frontend/src/views/property/PropertyView.vue` → `api/property.js` → `/api/admin/properties` → `property/controller/PropertyController` → `property/service/PropertyServiceImpl` → `property/mapper/PropertyMapper` → `mapper/stage1/PropertyMapper.xml` → `PropertyListVO`/`PageResult` | `countProperties` 与 `selectPropertyPage` 复用 `<include refid="propertyFrom/propertyWhere">`；contains `LIKE` 与 `LIMIT/OFFSET`；`total` 作为 JSON 数字序列化，页面 `PaginationBar.vue` 消费 total/page/pageSize。与上一条是**两套实际接口** |
| 居住人员 | `views/resident/ResidentView.vue` → `api/resident.js` → `/api/admin/residents` → `ResidentController` → `ResidentServiceImpl` → `ResidentMapper` → `mapper/stageb/ResidentMapper.xml` → `ResidentListVO` | current `residence.end_date IS NULL`，联 person/property_unit/楼栋，动态小区过滤；详情关系含责任住户与成员 |
| 账单与收费 | `views/billing/BillView.vue` → `api/bill.js` → `/api/admin/bills` → `BillController` → `BillServiceImpl` → `BillMapper` → `mapper/stage1/BillMapper.xml` → `BillListVO` | 读取 `v_bill_financial_summary` 再联房产层级、按范围分页；费用中心 `FeeCenterMapper.xml` 另读标准/付款/逾期，不应说已有在线收款写链 |
| Dashboard | `views/dashboard/DashboardView.vue` → `api/dashboard.js` 八组请求 → `DashboardController` → `DashboardServiceImpl` → `DashboardMapper/StatisticsMapper` → `mapper/stage1/{DashboardMapper,StatisticsMapper}.xml` | 数字/图表源于真实 SQL；下节逐项映射 |
| 户型图 | `AssetsView.vue`/`api/assets.js uploadFloorPlan` → `/api/v1/properties/{id}/floor-plan` → `FloorPlanController` → `FloorPlanService` → `FileObjectMapper` 及对象客户端 → MySQL `file_object + property_floor_plan`、MinIO `property-assets` | 文件流在 MinIO，元数据/关系在 MySQL；具体 SQL 在 `mapper/FileObjectMapper.xml`；模板变体由 `FloorPlanVariantMapper.xml` 读取 |
| 通知发送 | `views/notification/NotificationView.vue` → `api/notification.js` → `POST /api/admin/notifications` → `NotificationController` → `NotificationServiceImpl.send` → `NotificationMapper` → `mapper/notification/NotificationMapper.xml` | 事务内建立通知与多条 `notification_recipient`；收件范围最终解析为具体 person，而非伪造小区直接外键 |
| 车证审核 | `views/parking/ParkingPermitView.vue` → `api/stageH.js` → `PATCH /api/admin/parking-permits/applications/{id}/review` → `ParkingPermitAdminController` → `ParkingPermitServiceImpl.review` → `ParkingPermitMapper` → `mapper/parking/ParkingPermitMapper.xml` | 锁 person/property 行，校验资格，状态条件更新申请，批准时插入 permit；V8 CHECK/UK 是底线 |

MyBatis XML 语法的具体责任：`<include refid>` 在 `PropertyMapper.xml` 复用 JOIN 和 WHERE，防止 COUNT/分页筛选漂移；`<if>` 仅在有筛选值时加条件；`<choose>/<when>/<otherwise>` 用于受限排序、空 scope 显式拒绝；`<foreach>` 把授权 community ID 展开成参数化 IN 列表；`#{}` 是绑定参数，避免把用户输入直接拼成 SQL。`PropertyUnitMapper.xml` 的动态 ORDER BY 只从服务层允许的四个枚举列输出，而 `LIMIT/OFFSET` 仍用 `#{}`。页面总页数的后端字段在旧资产 `PagedResponse.totalPages` 明确计算；管理端 `PageResult` 仅给 `total/page/pageSize`，前端分页组件据此显示，不能互相混称。

## 7. Dashboard 的“页面数字从哪里来”

统一链前缀为 `frontend/src/views/dashboard/DashboardView.vue` → `frontend/src/api/dashboard.js` → `DashboardController` → `DashboardServiceImpl`；XML 为 `mapper/stage1/DashboardMapper.xml` 与 `StatisticsMapper.xml`。下列是页面**实际出现**的控件和方法，不是将来想做的图。

| 页面卡片/图表 | API 后缀 | Mapper 方法 / SQL 核心 |
|---|---|---|
| 管理房产、入住率 | `/overview?year=` | `DashboardMapper.selectOverview`：空间层级 COUNT、占用状态 COUNT/除法 |
| 本年应收/实收、待跟进欠费、收缴率 | `/overview?year=` | 同 `selectOverview`，聚合 `v_bill_financial_summary` 且按年份和作用域过滤 |
| 当前居住、主要居住人 | `/residence` | `selectResidenceDashboard`：有效 `residence`、`RESPONSIBLE` 数量 |
| 收费趋势（应收/实收/收缴率） | `/monthly-collection?year=` | `StatisticsMapper.selectMonthlyCollection`：财务 VIEW 联楼栋后按账月分组 |
| 费用构成 | `/fee-composition?year=` | `selectFeeComposition`：`bill→bill_item→fee_standard→fee_type`，按收费类别聚合 |
| 账单状态分布 | `/bill-status?year=` | `selectBillStatusDistribution`：`bill.status` 分组计数 |
| 房产占用分布 | `/occupancy` | `selectOccupancyOverview`：`property_unit.occupancy_status` 分组 |
| 居住关系/社区分布 | `/residence` | `selectResidenceRelationDistribution` / `selectResidentCommunityDistribution` |
| 待跟进账单 | `/overdue-top?limit=10` | `DashboardMapper.selectOverdueTop`：财务 VIEW `status='OVERDUE'`，按欠费降序 LIMIT |
| 当前挂牌、本期已出租挂牌、平均月租/挂牌天数、模式/状态图 | `/rentals?year=` | `selectRentalDashboard` 与 `selectRentalModeDistribution` / `selectRentalStatusDistribution`：`rental_listing`；“本期已出租”使用 `status='RENTED' AND YEAR(closed_at)=year`，不是合同签约日 |

`DashboardView.vue` 用 `Promise.all` 拉八组真实后端数据，ECharts 图是这些数组的可视化，没有 `Math.random()` 造数。其年份下拉当前只列 2026，是 UI 输入范围局限，不能误判统计 SQL 固定只支持 2026。Mapper 普遍带 `scopeBuilding` 小区过滤。

## 8. 索引、事务与并发：强项和边界

主键索引用于实体定位；唯一索引同时承载业务不变量（如 `bill(property_unit_id,billing_year,billing_month)`、`meter_reading(meter_id,reading_at)`、`notification_recipient(notification_id,person_id)`）；普通外键/查询索引服务连接与过滤。V6 的 `idx_bill_period_status` 是有明确账期/状态查询场景的一次增量，不要说项目每张表都做过专家级调优。`docs/STAGE_G_INDEX_AUDIT.md` 是历史 20 核心表/69 索引对象的审计样本，不能把这个数字误写成全库索引总数；Stage1 Runbook 的 208 条是 `information_schema.statistics` 索引列记录，二者统计口径不同。`docs/PPT_DATABASE_PERFORMANCE_AUDIT.md` 记录 MySQL 8.0.43、REPEATABLE-READ、autocommit=1 和一次历史只读 EXPLAIN/ANALYZE；这些是当时观察，不等于本轮复测。

典型乐观锁：`PropertyUnitService.update` 在事务中读旧行，`PropertyUnitMapper.xml` 用 `UPDATE ... SET version=version+1 WHERE property_unit_id=#{propertyUnitId} AND version=#{entity.version}`；更新 0 行触发冲突，阻止两个编辑者后提交者静默覆盖。社区、楼栋、单元也有同类版本更新；**不能推论每个业务表都加了乐观锁**。入住 `claimProperty/releaseProperty` 也使用房产 version 与占用状态条件。`AuthMapper.findRefreshSessionForUpdate` 按唯一 token hash 锁会话；`ParkingPermitMapper.lockPerson/lockProperty` 按 PK `FOR UPDATE`，审核通过后再写 permit。这些是代码可见的锁定读，是否发生过死锁要凭运行证据，现有审计没有证实死锁。

`@Transactional` 实例包括资产更新+审计、入住/退租、通知发出+收件关系、停车证审核、报事提交和文件元数据写入。真实历史回滚例在 `docs/PPT_TRANSACTION_ROLLBACK_EVIDENCE.md`：2026-08-30 入住事务中房产状态更新/居住插入后，停车资格原因码违反 CHECK，HTTP 500，事务内写入未残留；较早独立创建 person 的请求不会回滚。文件操作还有跨 MySQL/MinIO 一致性限制：`FloorPlanService` 使用 `afterCommit()` 延后清理旧对象，但 MinIO 上传本身不是数据库事务的一部分，不得称“跨存储原子提交”。`MeterSimulationServiceImpl` 的手动入口带事务；历史审计指出调度入口自调用事务方法可能不经代理，不能笼统称所有模拟读数路径都显式跨语句事务。

## 9. 文件元数据、恢复证据与未闭环项

`file_object` 记录 `bucket_name/object_key/original_name/content_type/size_bytes/sha256/status/uploaded_by_account_id` 等，`property_floor_plan.file_object_id`、`layout_template_floor_plan.file_object_id`、`layout_template_floor_plan_variant.file_object_id` 将对象关联具体业务；MinIO 持二进制。这避免把大文件塞入业务行，同时让数据库能做归属、唯一性和状态查询。`docs/DOCKER_STAGE1_RUNBOOK.md` 记载恢复时 `file_object=12`、`property-assets` 中对象=12，四张模板标准图、八张面积变体图均能在 MinIO 找到相同 bucket/key 和大小，并经普通 `compose down`（不带 `-v`）再 `up` 保持数据。该证据是当时的迁移验收，不是本轮在线对象巡检。

尚未闭环：①两 VIEW 及 `agency_staff`/`notification` 的原始构建来源不在现存十版 migration；②没有对当前在线库做本轮 metadata/行数查询；③没有重跑负向、性能或文件对象测试；④文档中的局部历史数字可能与之后业务数据变化不同。写最终报告时宜注明证据时间、采用完整备份最终 DDL 与现行源码，不以课堂截图替代 SQL 事实。
