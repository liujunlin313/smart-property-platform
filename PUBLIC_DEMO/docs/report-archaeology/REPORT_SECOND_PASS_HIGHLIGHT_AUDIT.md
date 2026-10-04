# 数据库课程设计报告：第二轮深度考古与亮点审计

> 审计对象：`C:\Projects\数据库课程设计\多小区智慧物业管理系统_PUBLIC_DEMO`。本文件是证据索引和写作判断，不是正式报告，也不是对现存数据库的修改建议。审计仅使用静态源码、已有 SQL/备份、阶段文档和既有日志；未执行写入数据库、业务测试、迁移或修复。以下相对路径均以该项目目录为根，**但数据库冷备位于同级目录** `C:\Projects\数据库课程设计\deployment_backups\community_db_before_public_demo_20260901.sql`；下文简称“最终备份”。第一轮四份材料见 `docs/report-archaeology/`，本轮只写增量发现，并在有冲突时纠正第一轮口径。

## 0. 先给结论：真正值得提高地位的内容

1. **把“关系约束如何表达现实时间状态”提高到报告主线。** 不止是 PK/FK：最终备份出现用 `CASE ... ELSE NULL` 的生成列配合唯一键限制当前产权/居住/挂牌/员工归属；但这些最终约束有一部分找不到对应 V1–V10 来源，必须将设计价值和版本来源缺口同时写出来。【SQL/备份直接证明】同级 `deployment_backups` 中的最终备份；【暂未找到证据】部分生成列的原始迁移。
2. **把“财务事实的真实性”提高到与 ER/范式并列的核心案例。** `bill_item` 把数量、当时单价和生成金额固定在明细，`payment` 是后续实收事实；对部分缴费无法归到具体收费项目时，通知逻辑显式标为“未分摊”，而不是编造项目欠费。这比笼统宣称“收费统计很复杂”更能说明建模选择。【源码直接证明】`backend/src/main/java/com/smartproperty/platform/notification/charge/ChargeNoticeComposer.java`；【SQL/备份直接证明】`database/migrations/V1__init_schema.sql`。
3. **把“迁移可重建性”从无条件优点改为有边界的优点。** 已有 V1–V10 历史和 Docker 恢复检验成立；但最终备份与迁移定义不完全同构，且 `agency_staff`、`notification`、两个 View 的初建来源缺失。最终部署恢复依赖完整备份，不应声称“只运行 V1–V10 即可还原现有全部库”。【SQL/备份直接证明】迁移、备份；【阶段文档证明】`docs/PUBLIC_DEPLOYMENT_PRECHECK.md`、`docs/DOCKER_STAGE1_RUNBOOK.md`。
4. **把两个真实一致性故事分开讲。** 2026-08-30 的入住 CHECK 失败及事务回滚是生产样式故障证据；Stage H 的双人并发入住 200/409 是阶段验证。二者不是同一故障，证明力度和时间也不同。【历史日志证明】`logs/run-20260830-163409-707-p34768/`；【阶段文档证明】`docs/PPT_TRANSACTION_ROLLBACK_EVIDENCE.md`、`docs/STAGE_H_IMPLEMENTATION_REPORT.md`。
5. **部署、地图、Redis 和日志保留为“数据库被真实使用/可追踪”的支撑，不抢数据库理论篇幅。** 地图有经纬度落库，但未发现 MySQL 空间类型/空间索引；Redis 有可用性/BYPASS 路径，未发现业务级缓存证据；短时负载不证明系统容量。【源码直接证明】【暂未找到证据】见下文分层判断。

## 1. 证据标注与版本边界

证据标签：`【源码直接证明】`＝现存可执行代码的静态行为；`【SQL/备份直接证明】`＝DDL、DML、最终 SQL 快照；`【历史日志证明】`＝已有运行时原始记录；`【阶段文档证明】`＝阶段报告/Runbook 的记载，未自动等同于本轮复测；`【合理推断】`＝由多条证据推出但未有原始实施记录；`【暂未找到证据】`＝已经检索指定范围仍未证实。**最终快照、V1 测试样例、阶段文档和当前源码不可无条件合并成“同一时刻的系统状态”。**

本轮检索了 `database/migrations`、`database/sample-data`、`database/evidence`、后端及 Mapper XML、前端页面与 API、`docs`、既有 `logs/run-*`、`deployment_backups`；还检查了同级原工程、`_PRE_STAGE1_RECOVERED`、`_MERGED_V1` 的相关 SQL/文档残留。没有把 README 或 PPT 作为唯一依据；这些旧目录均未提供可用的完整 Git 提交历史。【源码直接证明】文件树；【暂未找到证据】不可由此断言历史从未存在。

### 第一轮需要明确修正的一点：产权唯一约束

| 证据版本 | 实际规则 | 可推导结论 |
|---|---|---|
| `database/migrations/V3__phase2a_security_assets_and_files.sql:124-129` | `ownership_share=1`，当前房产 ID 生成列唯一 | **V3 当时**限制一房一个当前产权人。 |
| 同级 `deployment_backups` 中最终备份的 `ownership` DDL | `ownership_share>0 AND <=1`；唯一键为 `(person_id,current_property_unit_id)` | **最终备份**限制同一人对同一房产至多一条当前产权记录；不阻止同一房产多个不同当前产权人。 |

两者不可写成同一规则。最终结构**没有证明**存在份额之和必须为 1 的跨行约束，也不能因字段允许小于 1 就宣称实际已有共有产权案例。V3 到最终状态的对应迁移/手工变更来源在 V1–V10 及检索的旧 SQL 中未找到。【SQL/备份直接证明】【暂未找到证据】。建议正式报告用“V3 的阶段设计、最终备份状态及溯源缺口”三列展示，而不是沿用第一轮“一房仅一当前产权人是最终规则”的说法。

## 2. 《新增亮点 TOP 15》——按课程相关度、设计深度、证据完整度、答辩价值排序

每项的“证据”给出可追到的主要位置；后面闭环案例补足现实问题、为何不简化、页面、章节、展示和边界。

| 序 | 新增或被低估的亮点 | 主要证据与级别 | 建议报告位置 / 展示 |
|---:|---|---|---|
| 1 | **账单明细生成金额 + 当期单价快照**：避免数量、单价、金额三列相互矛盾；新费标不改历史账单 | 【SQL/备份直接证明】`database/migrations/V1__init_schema.sql` 的 `bill_item`、`fee_standard` | 第 3–4 章；主动展示 |
| 2 | **部分缴费不伪造收费项目余额**：账单级付款无法合法分摊到多个项目时保留“未分摊”语义 | 【源码直接证明】`notification/charge/ChargeNoticeComposer.java`、`ChargeNoticeMapper.xml` | 第 4、6 章；主动展示 |
| 3 | **最终“当前关系”生成列 + NULL 唯一语义**：居住人、责任住户、挂牌、员工归属按状态唯一 | 【SQL/备份直接证明】2026-09-01 最终 SQL；来源缺口见 §7 | 第 4–5 章；展示前说明版本边界 |
| 4 | **产权规则的实际演化而非静态 ER**：V3 与最终备份含义不同 | 【SQL/备份直接证明】V3 与最终备份 | 第 4–5 章；主动展示“为何要核对最终库” |
| 5 | **复合外键把出租授权与房产绑在同一业务对象上**，阻止把甲房授权用于乙房挂牌 | 【SQL/备份直接证明】`V1__init_schema.sql` 中 `fk_listing_authorization_property` | 第 3–4 章；主动展示 |
| 6 | **通知 N:M 收件事实 + 按收件人 JSON 财务上下文**，投递、已读不是通知主表单个状态 | 【SQL/备份直接证明】V4/V5；【源码直接证明】通知 Service | 第 3–4、6 章；可展示 |
| 7 | **多小区权限沿对象外键反查到 community，并在读/写两侧执行** | 【源码直接证明】`DataScopeServiceImpl.java`、Mapper XML；【阶段文档证明】`STAGE_H_R1...REPORT.md` | 第 5–6 章；主动展示 |
| 8 | **真实入住失败回滚与后来的条件冲突响应**，分别展示事务和并发保护 | 【历史日志证明】8 月 30 日 run；【阶段文档证明】PPT 证据、Stage H 报告 | 第 7 章；主动展示两条时间线 |
| 9 | **车证“资格→申请→审核→许可证”的事实拆分与审核锁** | 【SQL/备份直接证明】V8；【源码直接证明】`ParkingPermitServiceImpl.java` / XML | 第 3–4、6–7 章；有边界地展示 |
| 10 | **账单周期索引与 EXPLAIN 访问路径**：索引前缀对应跨房产的账期过滤，而非复用房产周期唯一键 | 【SQL/备份直接证明】V6；【阶段文档证明】`STAGE_G_SQL_PERFORMANCE_AUDIT.md` | 第 4、7 章；主动展示 |
| 11 | **表计读数按表+时刻唯一，模拟批量写入幂等**，历史读数不被最新值覆盖 | 【SQL/备份直接证明】V1；【源码直接证明】`MeterSimulationMapper.xml` | 第 3–4、6 章；可展示 |
| 12 | **一套查询条件同时支撑 count/page，排序白名单，空 scope fail-close** | 【源码直接证明】`stage1/PropertyMapper.xml`、`asset/PropertyUnitMapper.xml` | 第 5–6 章；主动展示 SQL 片段 |
| 13 | **年度图 12 月完整时间轴但“无记录≠0”**，避免用图表填充值篡改财务含义 | 【源码直接证明】`AnnualStatisticsView.vue`、`StatisticsMapper.xml` | 第 6–7 章；可展示 |
| 14 | **MySQL 元数据 + MinIO 对象、提交后旧对象删除**，明确跨系统补偿边界 | 【源码直接证明】`FloorPlanService.java`；【阶段文档证明】Stage E/Stage 1 对账 | 第 6、8 章；展示分工，不称原子事务 |
| 15 | **备份恢复核对的不只是行数**：迁移历史、View、约束、中文、对象键与 down/up | 【阶段文档证明】`DOCKER_STAGE1_RUNBOOK.md`；【SQL/备份直接证明】最终备份 | 第 8 章；选一张核对表展示 |

## 3. 可写成报告正文的 15 个“闭环故事”

下列每项依次回答：现实问题/不采用简单设计的原因；数据库动作；调用链与证据；可见效果与收益；推荐章节/答辩；不可夸大的边界。证据标签针对陈述而非整个题目。

### C01 费用标准变化后，历史账单为什么不应跟着改变

按小区、费用类型、生效时间维护标准；开账时将 `quantity`、`unit_price` 固定进 `bill_item`，`amount` 是 `ROUND(quantity*unit_price,2)` 的 STORED 生成列。若账单仅引用现行 `fee_standard`，后来调价会错误改写旧应收；若金额也人工维护，三列可能不一致。【SQL/备份直接证明】`database/migrations/V1__init_schema.sql` 中 `fee_standard` 的 `uk_fee_standard_version` 和 `bill_item` DDL；【源码直接证明】`backend/src/main/resources/mapper/stage1/BillMapper.xml` 的 `selectBillItems` 联费用类型、费标及前后读数。页面为 `frontend/src/views/billing/BillView.vue`；第 3–4、6 章，答辩宜展示。**边界：** 唯一“生效起日”不等于数据库排除了所有时间区间重叠；读数计费的完整开账过程不能仅从此 DDL 推出。

### C02 “部分缴费”为什么不能随便算到某一费用类型上

`payment` 关联账单，不含账单项分摊明细；多项目账单部分缴费时，按项目分配剩余欠费缺乏事实依据。通知实现把可精确计算与无法分摊的欠费拆为 `exactSelectedFeeOutstanding` / `unallocatedSelectedBillOutstanding`，批量 `selectDueRows` 避免逐收件人查询。【源码直接证明】`backend/src/main/java/com/smartproperty/platform/notification/charge/ChargeNoticeComposer.java`、`backend/src/main/resources/mapper/notification/ChargeNoticeMapper.xml`；【SQL/备份直接证明】V5 `notification_recipient.payload_json`、V1 payment/bill_item。`NotificationView.vue` 是发送入口；第 4、6 章，答辩强烈建议展示“诚实的空缺”。**边界：** 不等于实现了项目级付款分摊、在线付款或逐项目真实欠费结清。

### C03 当前/历史产权关系与规则演化

产权是带起止日、份额的人房关系，不应在 `property_unit` 放一个 `owner_id` 覆盖历史。V3 曾采用一房一个当前产权人，最终备份改成同一人房当前关系唯一并允许份额范围；这是必须核实的演化事实，而不是简单称“数据库天然支持多人共有”。【SQL/备份直接证明】V1/V3 与最终备份 `ownership`；【暂未找到证据】中间变更来源。`PropertyView.vue` → `PropertyController` → `PropertyServiceImpl` → `stage1/PropertyMapper.xml:selectCurrentOwners` 可显示当前关系。【源码直接证明】调用链。第 3–5 章，可展示版本对照；**边界：** 没有份额合计=1 的跨行数据库约束，也未证实最终快照实际有多产权人。

### C04 “当前居民”与“责任住户”不是同一字段

`residence` 保存人房、角色、起止时间，最终备份对当前居民和当前责任住户分别设置生成列唯一键；过往居住记录保留，不靠 UPDATE 覆盖。简单在 `person` 或 `property_unit` 存一个住户 ID 会丢失多人同住、搬入搬出和责任角色。【SQL/备份直接证明】最终备份 `residence` DDL；【源码直接证明】`residency/ResidenceManagementMapper.xml`、`ResidenceManagementServiceImpl.java`、`frontend/src/views/residency/MoveInView.vue`。第 3–4、6 章，值得展示。**边界：** 最终约束的原始迁移缺失；不能声称全时间段不重叠或每个房产只允许一个居民。

### C05 租赁授权不能错配房产

自售/中介挂牌状态不同；中介挂牌引用授权，复合 FK `(authorization_id,property_unit_id)` 强制授权和挂牌指向同一房产，阻止“拿甲房授权挂牌乙房”。【SQL/备份直接证明】V1 `rental_listing` 的 CHECK 与复合 FK；最终备份另有当前 PUBLISHED 房产唯一键，但来源未找到。【源码直接证明】`rental/RentalController.java`、`RentalServiceImpl.java`、`stageb/RentalMapper.xml`、`RentalView.vue`。第 3–4、6 章，可展示约束。**边界：** 当前挂牌唯一不等于所有申请阶段天然一对一；不要把前端展示说成完整中介签约闭环。

### C06 多小区角色与行范围是两套维度

角色控制动作，`account_community_scope` 控制可见社区；非全局账号的房产、账单、楼栋、居住等对象可沿 FK 反查 `community_id` 后校验。列表 SQL 中 `<foreach>` 限定 ID，空范围 `AND 1=0`；写入/审核路径也调用 `requireProperty` 等，不能只隐藏菜单。【SQL/备份直接证明】V4 scope 表；【源码直接证明】`access/service/impl/DataScopeServiceImpl.java`、`access/AccessControlMapper.xml`、`stage1/PropertyMapper.xml` 及 `ResidenceManagementServiceImpl.java`；【阶段文档证明】`STAGE_H_R1_IDENTITY_SCOPE_SECURITY_REPAIR_REPORT.md` 的 staff 范围与旧 API 封口。第 5–6 章，答辩主动展示账号→多条 scope→SQL。**边界：** 不能从几个样本证明项目每一条未来新增接口都被覆盖；旧 `/api/v1` 也要按自身鉴权检查，不能只看 `/api/admin`。

### C07 入住事务失败与并发冲突是两类问题

先写人房/房屋状态、后遇数据库 CHECK 失败，若没有事务会留下半办理状态。2026-08-30 的原始 run 和 PPT 复盘有 HTTP/SQL 错误与回滚证据。【历史日志证明】`logs/run-20260830-163409-707-p34768/access.log`、`backend.log`；【阶段文档证明】`docs/PPT_TRANSACTION_ROLLBACK_EVIDENCE.md`。Stage H 另有双员工同房并发入住验证：一方 200，一方 409，后续搬出后房屋版本递增到 2、无当前居住。【阶段文档证明】`docs/STAGE_H_IMPLEMENTATION_REPORT.md`；【源码直接证明】`ResidenceManagementServiceImpl.java` 和 `ResidenceManagementMapper.xml` 的事务/条件更新。`MoveInView.vue` 是入口；第 7 章，分两张时间线展示。**边界：** Stage H 是报告记录，未在本轮复跑；不能把一次场景演示说成形式化并发正确性证明。

### C08 通知为什么必须有收件人关系表

一条通知可给多人，某人也收多条；`recipient_role`、投递状态、已读时间是“这条通知发给这个人”的事实，不属于通知主表。V4 建 `notification_recipient`，V5 添每收件人上下文 JSON。【SQL/备份直接证明】V4/V5 与最终备份；【源码直接证明】`NotificationServiceImpl.java`、`NotificationMapper.xml`、居民门户查询 XML。`NotificationView.vue` 发送，`ResidentPortalView.vue` 查看。第 3–4、6 章，可展示 N:M 与 per-recipient 属性。**边界：** `notification` 主表的初建 DDL 缺失，不能声称 V1–V10 独立从零创建完整通知域；发送范围本身不是 Notification 对社区/房产的直接 FK。

### C09 车证是资格、申请和最终许可证，不是“车辆一张表”

V8 拆 `residence_parking_eligibility`、申请、许可证；申请快照含月份、计价，审核时 Service 锁 person、property，重新校验资格并条件更新 PENDING，再插许可证于事务内。简单在 vehicle 上加 `permit=true` 会失去申请/拒绝/有效期历史。【SQL/备份直接证明】V8；【源码直接证明】`parking/service/impl/ParkingPermitServiceImpl.java`、`parking/ParkingPermitMapper.xml`、`ParkingPermitView.vue`/`ResidentParkingPermitView.vue`；【阶段文档证明】`STAGE_UI_R2_PARKING_PERMIT_REPORT.md`。第 3–4、6–7 章，可展示状态与锁。**边界：** 最终库 `parking_space`、`parking_right` 为 0，车证是另一套业务；待审申请用前置 EXISTS 而非唯一键防重，存在并发竞态风险，不能宣称完全无重复申请。前端提供 `NEW_ENERGY`，而最终 `vehicle` CHECK 只允许 `CAR/MOTORCYCLE/OTHER`，为静态契约矛盾，未见运行失败证据，不能写成已复现故障。【源码直接证明】【SQL/备份直接证明】【暂未找到证据】

### C10 表计读数历史与同周期幂等

表计是房产与费用类型关联的设备，读数是 `(meter_id,reading_at)` 的时序事实；模拟器取最新读数、确定性计算增量、预查同时间桶，批量 `INSERT IGNORE`，同一桶重跑不堆新记录。【SQL/备份直接证明】V1 表计/读数键；【源码直接证明】`meter/service/impl/MeterSimulationServiceImpl.java`、`meter/MeterSimulationMapper.xml`；【阶段文档证明】`STAGE_H_IMPLEMENTATION_REPORT.md` 有首批 1584、重跑新增 0 的验证。`PropertyView.vue` 可看活跃表计，模拟入口是管理 API，并非独立图表页。第 3–4、6 章，可展示唯一键。**边界：** 15840 条是有规律的模拟读数，不是外部真实仪表采集；`scheduledTick()` 内部调用同类 `simulate()`，不能单凭注解断言定时路径一定经过 Spring 事务代理。

### C11 查询条件复用与分页真实性

房产筛选涉及社区、楼栋、楼层、状态、模糊匹配，count 与 page 共用 XML 条件块，避免“总条数与列表过滤不一致”；`LIMIT/OFFSET` 真分页；旧资产端动态排序用 `<choose>` 限定白名单，不拼用户原始字段。【源码直接证明】`stage1/PropertyMapper.xml:countProperties/selectPropertyPage` 与 `asset/PropertyUnitMapper.xml`。`PropertyView.vue` 与 `AssetsView.vue` 是不同链，不能混称；第 6–7 章，展示 SQL。**边界：** 深页 OFFSET 成本可能增加，未见游标分页或大规模深页压测。

### C12 账期索引为什么不是“已有唯一键就够了”

原唯一键 `(property_unit_id,billing_year,billing_month)` 从房产开始，适合单房按期定位；跨房产查某年某月与状态无法靠其前缀等效解决。V6 增 `(billing_year,billing_month,status,bill_id)`；阶段 EXPLAIN 记录代表性查询从约 603 行扫描/0.85ms 到 11 行/0.13ms。【SQL/备份直接证明】V1/V6；【阶段文档证明】`STAGE_G_SQL_PERFORMANCE_AUDIT.md`、`STAGE_G_INDEX_AUDIT.md`。`BillView.vue`、统计页受益；第 4、7 章，值得展示前后计划。**边界：** 测值是特定数据/SQL/环境，不是全站加速倍率；`information_schema.statistics` 208 是索引列行数，不是“208 个索引”，Stage G 对 20 张核心表计 69 个索引对象。

### C13 年度报表“未来月缺记录”和“真实零金额”的区分

后端只汇总数据库存在的账期，前端铺满 1–12 月横轴但缺月指标用 `null`，tooltip 提示暂无账单，而不是把缺月伪装成 0 元。【源码直接证明】`stage1/StatisticsMapper.xml:selectAnnualCollection/selectMonthlyCollection`、`statistics/service/impl/StatisticsServiceImpl.java`、`AnnualStatisticsView.vue`。月度页还可按社区下钻到明细。第 6–7 章，可展示数据库结果与图对照。**边界：** 补齐月份是展示层时间维度，不是新造出 12 个月账单；不能把年度轴 12 格当作 12 个月都完成出账。

### C14 文件替换如何跨 MySQL/MinIO 划边界

MySQL 保存 `file_object` 的 bucket/key/hash/content type/业务关联；MinIO 存二进制。上传新对象后写元数据并标旧记录，事务提交后再删旧对象；数据库异常时尝试删除新对象。【源码直接证明】`file/service/FloorPlanService.java` 的 `afterCommit`/`removeQuietly`、`FileObjectMapper.xml`；【SQL/备份直接证明】V3/V9/V10；【阶段文档证明】`STAGE_E_MINIO_FLOORPLAN_AUDIT.md`、`STAGE_UI_R4_FLOORPLAN_VARIANT_PREVIEW_FIX_REPORT.md`、Stage 1 对账。页面 `PropertyView.vue`/资产详情可预览，优先级为房产专属→布局面积变体→默认模板。第 6、8 章，可展示架构。**边界：** 删除补偿异常会被记/忽略，代码注释说明孤儿对象清理待完善；MySQL+MinIO 不具原子分布式事务。最终 `file_object` 12、默认模板 4、变体 12、房产专属关联 0，不能把模板行数加总成二进制对象数。

### C15 恢复验证为何比“容器重新启动成功”更重要

Stage 1 从冷备份恢复 MySQL/MinIO，验证表和真实中文、Flyway V1–V10 历史、2 View、约束/索引、12 个文件对象/对象存储对账，并执行 `down`（不用 `-v`）再 `up` 的持久化核对；Redis 不迁移业务数据。【阶段文档证明】`docs/DOCKER_STAGE1_RUNBOOK.md`；【SQL/备份直接证明】同级 `deployment_backups` 中的最终备份；【源码直接证明】`compose.yaml` 数据卷。第 8 章，答辩展示核对矩阵。**边界：** 证明该备份与当时流程可恢复，不证明纯迁移文件可无损重建最终库，也不证明长期备份轮转/异地灾备。

## 4. “老师问：这个数字哪里来的？”前端到 SQL 的追踪表

共同前缀：`frontend/src/api/http.js` 以 `/api` 为基础路径；表中 API 列写后续路径。Java 类均位于 `backend/src/main/java/com/smartproperty/platform/`，XML 位于 `backend/src/main/resources/mapper/`。同域多端点对应不同 XML，答辩前应按选定卡片/图的实际请求再核对一次；此表为静态路径，不冒充本轮浏览器实时抓包。【源码直接证明】

| 页面/要解释的值 | API → Controller → Service | Mapper/XML id → 主要表或 View → 返回/展示 |
|---|---|
| `AssetsView.vue` 层级/资产汇总 | `/v1/communities`、`/v1/buildings`、`/v1/properties`、`/v1/dashboard/summary` → `asset/*Controller` → `asset/*Service` | `asset/CommunityMapper.xml`、`BuildingMapper.xml`、`BuildingUnitMapper.xml`、`PropertyUnitMapper.xml`、`AssetDashboardMapper.xml` → community/building/building_unit/property_unit → 树和总数；**不是**管理端 `/admin/properties` 链。 |
| `PropertyView.vue` 总数/每行/详情 | `/admin/properties`、`/{id}` → `property/PropertyController` → `PropertyServiceImpl` | `stage1/PropertyMapper.xml:countProperties/selectPropertyPage/selectPropertyDetail/selectCurrentOwners/selectCurrentResidents/selectActiveMeters` → property_unit、空间层级、ownership、residence、meter → 总数、房屋、当前人员和表计。 |
| `ResidentView.vue` 人员与居住 | `/admin/residents`、`/summary`、`/{residenceId}` → `resident/ResidentController` → `ResidentServiceImpl` | `stageb/ResidentMapper.xml:countResidents/selectResidentPage/selectResidentSummary/selectResidentDetail` → residence/person/property_unit/community → 分页数、居住状态。 |
| `MoveInView.vue` 入住/搬出 | `/admin/residence-management/...` → `ResidenceManagementController` → `ResidenceManagementServiceImpl` | `residency/ResidenceManagementMapper.xml` → person/ownership/residence/property_unit → 可选房源、办理结果、版本冲突。 |
| `BillView.vue` 账单/项目/付款 | `/admin/bills`、`/{id}` → `billing/BillController` → `BillServiceImpl` | `stage1/BillMapper.xml:countBills/selectBillPage/selectBillDetail/selectBillItems/selectPayments` → bill、bill_item、payment、fee_type/standard、meter_reading、财务 View → 应收、实收、欠费和明细。 |
| `FeeCenterView.vue` 费标/付款/逾期 | `/admin/fees/types|standards|payments|overdue` → `FeeCenterController` → `FeeCenterServiceImpl` | `stageb/FeeCenterMapper.xml:selectActiveFeeTypes/countFeeStandards/selectFeeStandardPage/countPayments/selectPaymentPage/selectOutstandingAmount/countOverdueBills/selectOverdueTopProperties` 等 → fee_type/standard、bill/payment、财务 View → 费标和欠费。 |
| `DashboardView.vue` 八组卡/图 | `/admin/dashboard/overview|monthly-collection|fee-composition|bill-status|occupancy|overdue-top|residence|rentals` → `DashboardController` → `DashboardServiceImpl` | `stage1/DashboardMapper.xml:selectOverview/selectOverdueTop/selectResidenceDashboard/selectRentalDashboard` 等；统计项复用 `StatisticsMapper.xml` → bill、payment、residence、rental_listing、View → `dashboard.js` 的真实 API 数据；页面 `Promise.all`，无随机统计。 |
| `MonthlyStatisticsView.vue` 月→社区→明细 | `/admin/statistics/monthly|monthly/communities|monthly/drilldown` → `StatisticsController` → `StatisticsServiceImpl` | `stage1/StatisticsMapper.xml:selectMonthlyCollection/selectMonthlyCommunities/selectMonthlyDetailSummary/selectMonthlyFeeComposition/selectMonthlyBillStatusDistribution` 等 → bill/payment、社区、财务 View → 每月/每社区财务指标。 |
| `AnnualStatisticsView.vue` 年度 | `/admin/statistics/annual` → 同上 | `StatisticsMapper.xml:selectAnnualCollection` 等 → 真实账期聚合 → 年度卡/12 月图；缺月份由 Vue 置 `null`。 |
| 多小区作用域检查 | `/admin/access/community-scope` → `AccessScopeController` → `AuthorizationService` + `DataScopeService` | `access/AccessControlMapper.xml:selectScopedCommunities` → account_community_scope/community → 可管理小区；其他业务 Mapper 的动态 IN 与对象 `require*` 共用该范围。 |
| `MapView.vue` 坐标/Marker | `/admin/map/scene` → `MapController` → `MapSceneServiceImpl` | `map/MapSceneMapper.xml:selectCommunityMapPoints/selectBuildingMapPoints` → community/building/property_unit 经纬度和范围 SQL → 点位/数量；`/locations/sync-missing` 是另一条写链，非高德每次直驱页面。 |
| `RentalView.vue` 挂牌与维度统计 | `/admin/rentals`、`/summary`、`/agencies`、`/{id}` → `RentalController` → `RentalServiceImpl` | `stageb/RentalMapper.xml:countRentals/selectRentalPage/selectRentalDetail/selectSummary/selectCommunityDistribution/selectModeDistribution/selectAgencyDistribution` 等 → rental_listing/authorization/agency/property_unit → 列表、模式/中介统计。 |
| `ParkingPermitView.vue` / `ResidentParkingPermitView.vue` | `/admin/parking-permits/...`、`/resident/me/parking-permits/...` → `ParkingPermitAdminController` / `ResidentParkingPermitController` → `ParkingPermitServiceImpl` | `parking/ParkingPermitMapper.xml:countApplications/selectApplicationPage/countPermits/selectPermitPage/selectEligibilities/selectMyApplications/selectMyPermits` → 申请、许可、居住/产权/车辆 → 待办、审核、个人证；不是 `parking_space` 占用统计。 |
| 房产详情表计 + 模拟入口 | `/admin/properties/{id}` 与 `/admin/meter-simulator/simulate-now` → `PropertyController` / `MeterSimulationController` → 对应 Service | `stage1/PropertyMapper.xml:selectActiveMeters`；`meter/MeterSimulationMapper.xml:selectActiveMeterStates/selectExistingMeterIds/insertReadings` → meter/meter_reading/fee_type → 历史与新增读数；不存在独立“表计图”页面的证据。 |

答辩演示时先抓定一个页面的请求，再展示该 Controller、Service、Mapper id 和 SQL 条件，最后展示相关表/数据行；不要拿不同账期、不同角色、不同快照的数字硬对比。完整 HTTP→SQL 日志只作为这条链的运行旁证，参见 `docs/DOCKER_STAGE2_5_OBSERVABILITY_RUNBOOK.md` 和对应 `logs/run-*`。【阶段文档证明】

## 5. 数据规模、关系真实性与“不宜吹大数据”

【SQL/备份直接证明】最终快照及【阶段文档证明】`docs/PPT_FINAL_STATE_AUDIT.md`：4 小区、14 楼、40 单元、528 房、606 人、528 产权、597 居住；1584 表计、15840 读数；3696 账单、18273 账单项、3324 付款；80 挂牌、64 授权、3 中介、10 员工；7 通知、507 收件关系；5 车辆、5 申请、3 许可证。业务行并非一张孤立大表：房产→表计/账单/产权/居住，账单→账单项/付款，通知→收件人都有 FK/调用链。`3696=528×7`、`15840=1584×10` 显示周期性的规则化构造。【合理推断】这是有业务关系的演示数据，但不能宣称是自然随机分布的真实物业生产数据、海量数据或长期仪表采集。当前可见 V1 `database/sample-data/01_demo_data.sql`、V8 少量确定性 DML、Stage H 表计模拟器；**完整生成 528 房等全量数据的脚本/算法尚未找到**。【暂未找到证据】

还有一处必须区分：V1 种子断言验证过合同/停车/设施等初始样例，但最终 PUBLIC_DEMO 快照中 `lease_contract`、`contract_participation`、`parking_space`、`parking_right`、`spatial_boundary`、`facility_placement`、`property_floor_plan`、`account_agency_role` 为 0。【SQL/备份直接证明】`docs/PPT_FINAL_STATE_AUDIT.md` 与最终 dump。不能拿旧 seed 的“曾有记录”宣称最终演示库含完整合同、车位或手绘边界场景；“支持的数据结构”和“当前有数据可演示”应分别列。

## 6. 演进与真实困难：只讲证据能支撑的历史

| 变化/问题 | 可核实演进 | 证据级别与不能越界之处 |
|---|---|---|
| V1：基础业务图谱 | 社区→楼栋→单元→房产、人房、租赁、账务、表计、车辆/停车、通知关系的基础模型；负向约束和种子断言伴随出现 | 【SQL/备份直接证明】`V1__init_schema.sql`、`database/tests/`、`database/evidence/`。某些旧业务表最终为空。 |
| V2：参考数据 | 建模之外需要稳定的类型/基础数据供查询和 UI 使用 | 【SQL/备份直接证明】`V2__seed_reference_data.sql`；不要称为新业务表迁移。 |
| V3：安全、资产、文件 | 账号/角色和文件元数据及户型图能力扩充；产权当时施加一房当前唯一 | 【SQL/备份直接证明】V3。**不是最终产权规则**。 |
| V4：多社区 scope、通知、空间边界 | `account_community_scope`、权限表、空间边界、通知改表与收件表 | 【SQL/备份直接证明】V4。V4 注释预设 notification 已存在，初建缺口不能略去。 |
| V5：收件人财务负载 | `notification_recipient.payload_json` 允许每人单独保存消息数据 | 【SQL/备份直接证明】V5；业务用途【源码直接证明】通知 composer。 |
| V6：账期状态索引 | 对跨房产账期筛选补访问路径 | 【SQL/备份直接证明】V6；【阶段文档证明】Stage G EXPLAIN。 |
| V7：居住期限/居民报事 | 预期结束日与独立事件表，防止把报事写在 residence 状态字段中 | 【SQL/备份直接证明】V7。 |
| V8：车证申请/许可证 | 三表、FK/CHECK/索引及权限；**文件还含 INSERT 示例数据** | 【SQL/备份直接证明】V8。迁移混入示例数据，正式报告可作为“迁移职责边界”反思。 |
| V9/V10：户型图从默认模板到面积变体 | 先模板关系，再加 `layout_template_floor_plan_variant`，无需给每套房复制文件 | 【SQL/备份直接证明】V9/V10；【阶段文档证明】Stage UI-R4。 |
| 2026-08-30 入住失败 | 请求中先创建人后办理入住，后一步触发 CHECK 3819，内部多步回滚 | 【历史日志证明】对应 run；【阶段文档证明】`PPT_TRANSACTION_ROLLBACK_EVIDENCE.md`。需区分“创建独立人记录成功”与“入住事务内部写入回滚”，不能说全部 HTTP 请求原子化。 |
| Stage H-R1 身份/范围封口 | 员工与居民旧 API 边界被审查与修复，scope 的真实返回不再只靠菜单 | 【阶段文档证明】`STAGE_H_R1_IDENTITY_SCOPE_SECURITY_REPAIR_REPORT.md`；【源码直接证明】当前权限/Mapper。不要用阶段总结推断所有接口永不越权。 |
| Docker 恢复/中文/对象对账 | MySQL+MinIO 恢复、中文记录与对象键核验，容器 down/up 验证数据卷 | 【阶段文档证明】Stage1 Runbook；不是凭 Docker 技术栈名称推断。 |
| 日志 Run Session 分类 | Stage2.6 记录了分类修正，说明观测数据也需定义“本次运行”边界 | 【阶段文档证明】`STAGE2_6_RUN_CLASSIFICATION_REVIEW.md`。这是工程支撑，不是数据库理论创新。 |

本轮还发现一个**尚未有运行故障证明、但值得开发待办记录的静态契约矛盾**：前端车类型 `NEW_ENERGY`，服务端可传入该值，最终 `vehicle` CHECK 允许值不含它。不能把它写成“曾遇到且已修复的 Bug”；也不应借审计擅自改 SQL。【源码直接证明】【SQL/备份直接证明】【暂未找到证据】实际失败日志。

## 7. 《仍未解决的证据缺口》与报告措辞

| 缺口 | 已核查结果 | 正式报告应如何写 |
|---|---|---|
| “ID/字段 32→64”真实历史 | 检索 V1–V10、四工程旧文件、docs/logs 与备份，未定位一条明确的字段、原错误、变更 SQL 的闭环；简单同名 VARCHAR(32→64) 比对也未给出直接对应 | 【暂未找到证据】不写成事实；仅列口述待原始材料确认。 |
| `agency_staff` 初建 | 最终备份有 CREATE TABLE 和当前员工唯一键；V1–V10 未找到 CREATE，其他旧目录未找到原始迁移 | 【SQL/备份直接证明】最终存在；【暂未找到证据】初建来源。 |
| `notification` 初建 | V4 对该表 `ALTER`，原 CREATE 仅在最终备份查到 | 同上；V4 不能独立在全新仅有 V1–V3 的库上自然应用。 |
| `v_bill_financial_summary`、`v_monthly_collection_summary` 初建 | 最终备份含 VIEW 定义，历史 V1–V10 没找到 CREATE VIEW；Stage1 恢复验证两视图 | 可讲最终 VIEW 和恢复；【暂未找到证据】何时初建/改口径。 |
| V3→最终产权、当前居住/责任住户、当前挂牌、当前员工生成唯一键 | 最终备份有这些 DDL，部分与 V3 相冲突/超出 V1–V10；原始变更脚本未找到 | 单设“Schema 演进可追溯性不足”，不把最终 DDL 强行归到某版迁移。 |
| 528 房等全量数据发生器 | 找到局部 seed、V8 示例、表计模拟、最终备份；没有完整可复现生成脚本 | 说“最终备份具有该规模”，不说“按某随机算法生成”。 |
| 数据库账号 GRANT、DDL/DML 分权 | Compose/连接配置显示应用账号；未找到可证实独立迁移账号及实际 GRANT 的 SQL/`SHOW GRANTS` 证据 | 【暂未找到证据】不能宣称最小权限已经落实。 |
| 触发器、存储过程、MySQL POINT/SPATIAL INDEX | 现有迁移、最终备份和相关 Mapper 中未找到 | 不将这些列作已有成果；地图用普通经纬度/JSON 与外部 API。 |
| `spatial_boundary` 的对象完整性 | `subject_type+subject_id` 多态唯一，未有指向 community/building 的普通 FK；最终快照 0 行 | 可写“预留的边界结构”，不能写成已验证空间边界检索规模案例。 |
| 性能与缓存 | Stage G 有代表性 EXPLAIN；Redis 有健康/BYPASS，未见业务 `RedisTemplate`/`@Cacheable` 实际命中链；短负载测试不能推容量 | 分别写索引证据、基础设施就绪和未知容量；不得混称“缓存使查询快 N 倍”。 |
| 合同/车位/在线缴费写链 | 初始模型有表，最终部分快照为空，前端现有链主要是查看/申请/审核；未找到闭环在线收款和正式合同签约证据 | “结构预留/部分能力”，不把表存在当作功能完成。 |

**优先后续人工找证据的顺序：** 当时的手工 SQL/Workbench 历史或旧备份差异 → 迁移外生成的 schema 记录 → 原始数据批量生成脚本或终端日志 → `SHOW GRANTS` 的历史导出。若找不到，正式报告保留缺口比虚构迁移更有说服力。此处不建议在本次只读审计中补 migration 或重建数据库。

## 8. 哪些能称“数据库课程亮点”，哪些只能做支撑

| 层级 | 可写内容 | 控制措辞 |
|---|---|---|
| **核心数据库亮点** | 时态人房关系、复合 FK、当前态生成唯一、生成金额、账单事实拆分、部分付款语义、约束负向测试、DataScope SQL、View/聚合、联合索引与计划、事务/锁、迁移和最终结构差异 | 每例给具体键/SQL/测试；最终 DDL 与历史迁移严格分开。 |
| **数据库支撑工程** | MyBatis XML 动态 SQL、分页/排序、MySQL↔MinIO 元数据对账、Docker 卷与完整备份恢复、HTTP→Mapper→SQL 日志、账期/角色 API 现场追踪 | 证明数据库设计在运行，不宣称这些本身是范式创新。 |
| **普通系统工程** | Spring Security/JWT、Vue 组件、Nginx、Docker 镜像、AMap JS/Web Service、CORS/WLAN/Quick Tunnel、UI 改版 | 简述其如何读写/保护数据；不按技术名词堆篇幅。 |
| **不能作为已证成果宣称** | Redis 业务缓存加速、生产级大规模负载、MySQL 空间索引、跨 MySQL/MinIO 原子事务、无缺口的 Flyway 从零重建、完整在线缴费/合同/车位业务、数据库账号最小 GRANT、全部端点永不越权 | 对这些是“未证实/未实现/仅局部验证”，不是否认代码中预留结构。 |

## 9. 对 3–4 万字大纲的增量修订建议

保留第一轮 `REPORT_OUTLINE_30K_40K.md` 的九章总框架和数据库为中心的字数重心，不重新写一套目录。建议微调：第 3 章增加“关系上的属性与时间性”（产权、居住、通知收件、授权）；第 4 章增加“生成列唯一与最终快照/迁移差异”以及“生成金额与项目级未分摊语义”；第 5 章将“Flyway 版本清单”改为“需求驱动迁移 + 来源缺口 + 恢复边界”，明确最终库不是现存脚本自给自足；第 6 章用 §4 的追踪表择 3–4 条讲透而不是罗列全部页面；第 7 章并列展示 CHECK 回滚、200/409 并发、V6 索引计划，标注证据日期；第 8 章以 MySQL/MinIO/中文/VIEW 对账说明数据生命周期，Redis/地图/公网只作配角。附录保存完整追踪矩阵与未解决缺口，不用工程名词灌满正文。

最终最有说服力的汇报顺序建议：**一条现实规则 → 一条 DDL 约束/关系 → 一条 Mapper SQL → 一个页面数值 → 一条历史失败或验证记录 → 这个设计仍然解决不了什么。** 这一顺序比“用了 Spring Boot、Vue、Docker、Redis……”更符合数据库课程评价，也更能如实解释“数据库为什么变成今天这样”。
