# 课程设计报告证据地图与截图清单

> 路径除单独标明外均相对 `C:/Projects/数据库课程设计/多小区智慧物业管理系统_PUBLIC_DEMO/`。这是**素材索引**，不是新生成的运行测试。截图应遮盖姓名、手机号、证件号、账号散列、Token、密码、密钥与真实私有对象路径。引用既有测试结论时必须写其执行日期/版本，不标成“本轮验证”。

## A. 图、关系模型、物理结构

| 优先级/建议章节 | 证据位置与建议截取 | 可证明什么；注意什么 |
|---|---|---|
| 高 / 需求与概念 ER | `docs/ppt_assets/global_er_concept.html` 或 `.svg`；截完整主图 | 现实世界的空间、人员、收费主关系；不应写上 PK/FK 后称概念图 |
| 高 / 业务 ER 分解 | `docs/ppt-er/01_global_core_er.html`～`06_resident_service_notification_er.html`；各取主体，配 `docs/PPT_ER_MODEL_AUDIT.md` | 资产层级、权属/居住、收费、车位、通知 N:M；选择能讲清关系的子图，不必六图全部正文 |
| 高 / 关系模式 | `docs/database/RELATION_SCHEMA_V1.md`、`NORMALIZATION_REPORT_V1.md` | V1 的关系模式和范式推导；须说明后续 V3–V10 演进，不能当最终全库模式 |
| 高 / 当前物理结构 | `C:/Projects/数据库课程设计/deployment_backups/community_db_before_public_demo_20260901.sql` 中 `CREATE TABLE property_unit/ownership/residence/bill/bill_item/payment/notification_recipient`；或已有 Workbench 历史截图 | 最终备份的 PK/UK/FK/CHECK/索引；若做新的 Workbench 截图，应另获授权，不能由本轮只读报告假称已经拍摄 |
| 高 / VIEW 结构 | 同一 dump 尾部 `Final view structure for view v_bill_financial_summary` 和 `v_monthly_collection_summary` | 应收/成功实收/欠费两层聚合；**不要截中部 Temporary view structure 的 `1 AS` 占位语句** |

## B. 版本演进、约束与测试

| 优先级/章节 | 文件/截图范围 | 报告中的证据命题 |
|---|---|---|
| 高 / Flyway | `database/migrations/V1__init_schema.sql`、`V3__phase2a_security_assets_and_files.sql`、`V4__stage_f_access_scope_boundary_notification.sql`、`V6__stage_g_bill_period_status_index.sql`、`V8__parking_permit_management.sql`、V9/V10 | 用前后几个代表性版本讲结构演进；完整十版列表见 `DATABASE_DESIGN_ARCHAEOLOGY.md`；V8 有演示 INSERT，勿说迁移只含 DDL |
| 高 / 迁移历史 | 备份 `INSERT INTO flyway_schema_history`（不要整行泄露其他数据）；`docs/DOCKER_STAGE1_RUNBOOK.md` 的 V1–V10 验证段 | 恢复库确有 10 条成功历史；`application.yml`/`compose.yaml` 又证明当前自动迁移关闭，二者不矛盾 |
| 高 / FK/UK/CHECK | `V1__init_schema.sql` 的 `building`、`property_unit`、`bill`、`bill_item`，`V3` 的当前业主生成列/唯一键，`V8` 的停车资格/报价约束 | 业务不变量如何落实到数据库；图上应标出约束名和对应失败例，而非堆整页 DDL |
| 高 / 负例 | `database/tests/03_constraint_negative_cases.sql` 的 NEG-01～16 + `database/evidence/07_negative_cases.txt` + `VERIFICATION_SUMMARY.txt` | 16 个预期错误在 2026-08-21 V1 隔离验证中观察到；只可称历史 V1 验证，不是当前 46 表全套回归 |
| 高 / seed 断言 | `database/tests/05_seed_assertions.sql` A01～14 + `database/evidence/09_seed_assertions.txt` | 种子数据不仅“能插入”，还满足跨小区拥有/居住区别、三元授权、账单项/读数关系；14 PASS |
| 中 / 数据规模 | `docs/PPT_DATABASE_PERFORMANCE_AUDIT.md` §2、`docs/DOCKER_STAGE1_RUNBOOK.md` 恢复核对 | 日期化的 4 小区、14 楼、528 房、3,696 账单等；不要写成 2026-09-24 在线行数 |
| 中 / 性能索引 | `database/migrations/V6__stage_g_bill_period_status_index.sql`、`docs/STAGE_G_INDEX_AUDIT.md`、`docs/PPT_DATABASE_PERFORMANCE_AUDIT.md` | 账期-状态索引理由与历史 EXPLAIN；69 个是抽样 20 表的索引对象，208 是全库统计条目，不可混称 |
| 中 / 事务真实反例 | `docs/PPT_TRANSACTION_ROLLBACK_EVIDENCE.md` 及其指向的历史 `logs/run-20260830-163409-707-p34768/` | 入住事务因 CHECK 失败回滚；人员创建是单独先完成的请求，不参与回滚 |

## C. 账号作用域与可运行数据链

| 优先级/章节 | 代码/报告位置 | 推荐图和证明 |
|---|---|---|
| 高 / 作用域关系表 | V3 `app_account/app_role/account_global_role`，V4 `app_permission/role_permission/account_community_scope`；备份对应 CREATE TABLE | 一账号可管理多个小区、一个小区可被多账号管理的关系；避免展示密码散列 |
| 高 / Java 作用域 | `backend/src/main/java/com/smartproperty/platform/access/service/{AuthorizationServiceImpl,DataScopeServiceImpl}.java`；`auth/context/ActorContextProvider.java` | 当前 JWT 角色→权限码→授权社区集合；系统管理员 global 分支，普通员工无范围时不会放行 |
| 高 / XML 作用域 | `backend/src/main/resources/mapper/access/AccessControlMapper.xml`，`mapper/stage1/PropertyMapper.xml`、`StatisticsMapper.xml`、`stageb/ResidentMapper.xml` | `status='ACTIVE'` 取范围、`<foreach>` 参数化 `IN`、无授权时 `AND 1=0`；截一条短 SQL 即可 |
| 高 / 范围对照 | `docs/STAGE_G_IDENTITY_PROVISIONING_REPORT.md`、`docs/STAGE_H_R1_IDENTITY_SCOPE_SECURITY_REPAIR_REPORT.md` | `staff_scope_a` 社区 4，`staff_scope_c` 社区 4/6；历史房产/Dashboard 72 对 168、居民旧资产 API 403 |
| 高 / 房产旧资产全链 | `frontend/src/views/AssetsView.vue`、`frontend/src/api/assets.js`、`asset/controller/PropertyUnitController.java`、`asset/service/PropertyUnitService.java`、`asset/mapper/PropertyUnitMapper.java`、`mapper/asset/PropertyUnitMapper.xml` | count 与分页同条件、LIMIT/OFFSET、动态排序、version 更新；适合画“浏览器→SQL”示意 |
| 高 / 管理端房产全链 | `views/property/PropertyView.vue`、`api/property.js`、`property/controller/PropertyController.java`、`property/service/impl/PropertyServiceImpl.java`、`property/mapper/PropertyMapper.java`、`mapper/stage1/PropertyMapper.xml` | `/api/admin` 的 Query、VO、Count、DataScope；不要与旧 `/api/v1` 混为一套 |
| 高 / Dashboard 图表 | `frontend/src/views/dashboard/DashboardView.vue`、`api/dashboard.js`、`dashboard/controller/DashboardController.java`、`dashboard/service/impl/DashboardServiceImpl.java`、`mapper/stage1/DashboardMapper.xml`、`StatisticsMapper.xml` | 指向顶部数字、月度趋势、费用构成、状态、入住/租赁/逾期真实 SQL；见数据库考古 §7 映射表 |
| 中 / 居民、账单、车证、通知 | `mapper/stageb/ResidentMapper.xml`、`mapper/stage1/BillMapper.xml`、`mapper/parking/ParkingPermitMapper.xml`、`mapper/notification/NotificationMapper.xml` 及同名域 Controller/Service | 独立业务链与复杂 JOIN/事务；选 2–3 条入正文，其余附录 |

## D. 存储、工程与已有运行报告

| 优先级/章节 | 位置 | 建议用途 |
|---|---|---|
| 中 / MySQL–MinIO 元数据 | `V3__phase2a_security_assets_and_files.sql` 的 `file_object/property_floor_plan`、V9/V10 模板图；`file/service/FloorPlanService.java`；`docs/STAGE_E_MINIO_FLOORPLAN_AUDIT.md` | DB 管归属和元数据、MinIO 存二进制；`FloorPlanService` afterCommit 清旧对象，但不是分布式原子事务 |
| 中 / 对象迁移验证 | `docs/DOCKER_STAGE1_RUNBOOK.md` §恢复验证 | 当时 12 条 `file_object` 与 MinIO 12 对象 bucket/key/大小对应；`down`/`up` 不带 `-v` 后仍保留 |
| 中 / 持久化边界 | `compose.yaml` 中 `public_demo_mysql_data` 和 `public_demo_minio_data`，`docs/DOCKER_STAGE2_RUNBOOK.md` | 容器生命周期与数据卷分离；Docker 作为附录或实现章，不抢关系设计篇幅 |
| 中 / SQL 观测 | `docs/DOCKER_STAGE2_5_OBSERVABILITY_RUNBOOK.md`、`logs/` 已有某次 `access.log/backend.log`、`docs/PPT_TRANSACTION_ROLLBACK_EVIDENCE.md` | 可截一次脱敏 HTTP→Controller→Service→Mapper→SQL→响应时间链；日志仅是执行证据，不代替 DDL |
| 低 / 压测 | `logs/run-20260902-172714-manual-deploy-backend/tests/system-load-20260902-223609/system-load-report.{json,html}` 与 `database-load-20260902-223806/`；`scripts/system-load-test.ps1`、`database-load-test.ps1` | 日期化历史场景结果；数据库场景含登录/会话写入，不能说“只读”；正文一小节或附录 |

## E. 不宜截图/不宜声称

不展示 `.env.docker`、`deploy/local.env.ps1`、JWT/MinIO/高德凭据、`app_account.password_hash`、token 表数据、居民个人信息。不要在报告中直接贴整个 SQL dump。不要截 dump 的临时 VIEW 占位。不可声称 V1 负例覆盖 V10，不可将 Flyway 说成当前启动自动迁移，不可把历史负载场景的成功率说成全系统无故障，不可用 PPT 概念图代替最终物理结构证明。若需要“当前在线数据库”截图，本轮没有生成；后续须以新的只读、脱敏取证另做。
