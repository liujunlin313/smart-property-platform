# 3–4 万字数据库课程设计报告三级大纲（仅骨架，不是正文）

> 建议正文约 **35,000 字**，表格与图题、代码附录不计或按学校规则另计。下面每个三级节给出“要回答的问题｜可信素材｜建议字数｜图/代码｜与数据库课程的关系”。写作时优先引 `DATABASE_DESIGN_ARCHAEOLOGY.md` 和 `REPORT_EVIDENCE_MAP.md` 的证据，不按项目常见做法脑补。所有历史测量注明日期。工程/地图/观测只作支撑，数据库模型、约束、查询、测试占主体。

## 第 1 章 绪论与研究边界（2,400 字）

| 三级节 | 要回答的问题 | 真实素材 | 字数 | 建议图/表 | 数据库关联 |
|---|---|---|---:|---|---|
| 1.1.1 多小区物业信息的统一管理问题 | 为何单一社区的表格管理难以处理跨小区房产、住户、收费？ | `PRODUCT.md`、`DESIGN.md`、`community/building/property_unit` DDL | 800 | 场景与数据对象图 | 确定数据域和实体边界 |
| 1.2.1 课程目标、范围与非目标 | 报告评估什么，哪些并未实现？ | `README.md`、前端路由、`docs/PPT_DATABASE_PERFORMANCE_AUDIT.md` 未有在线缴费写链结论 | 800 | 范围表 | 避免功能宣传替代数据设计 |
| 1.3.1 方法与证据版本 | 当前 PUBLIC_DEMO、历史备份、V1 测试怎样区分？ | 本次四份考古文档、2026-09-01 dump、`database/evidence` | 800 | 证据时间轴 | 保证结构、测试结论可追踪 |

## 第 2 章 需求分析与数据流（3,200 字）

| 三级节 | 要回答的问题 | 真实素材 | 字数 | 建议图/表 | 数据库关联 |
|---|---|---|---:|---|---|
| 2.1.1 角色与跨小区范围 | 管理员、物业员工、居民读什么数据？ | `SecurityConfig`、`ActorContextProvider`、`STAGE_H_R1...REPORT.md` | 800 | 角色×数据范围矩阵 | 范围不是 UI 菜单权限 |
| 2.2.1 资产与人房关系需求 | 房产层级、拥有、居住为何分开？ | V1/V3 DDL、`PropertyMapper.xml` | 800 | 空间链与人房关系图 | 识别实体与联系 |
| 2.2.2 账务/租赁/通知/车证数据流 | 哪些是独立事实、哪些是关联？ | V1、V4、V8，相关 Service | 800 | 业务数据流图 | 为中间实体和事务设边界 |
| 2.3.1 非功能与保护边界 | 一致性、权限、历史、文件、可恢复性要怎样落库？ | CHECK/FK/UK、`compose.yaml`、Stage1 Runbook | 800 | 质量要求表 | 引出约束、版本、备份 |

## 第 3 章 概念结构设计（4,700 字）

| 三级节 | 要回答的问题 | 真实素材 | 字数 | 建议图/表 | 数据库关联 |
|---|---|---|---:|---|---|
| 3.1.1 全局概念 ER 与主题划分 | 核心业务对象与关系是什么？ | `docs/ppt_assets/global_er_concept.html`、`PPT_ER_MODEL_AUDIT.md` | 900 | 全局概念 ER | 概念层不放 PK/FK |
| 3.2.1 小区—楼栋—单元—房产 | 各层为何独立、为何房产为锚？ | `docs/ppt-er/01_global_core_er.html`、V1 DDL | 900 | 空间子 ER | 层级 1:N |
| 3.2.2 人员—产权—入住 | 人、业主、住户、租户如何不混淆？ | `docs/ppt-er/03_person_ownership_residence_er.html`、Ownership/Residence DDL | 1000 | 人房子 ER | N:M 关系实体化与时间性 |
| 3.2.3 账单—项目—付款—表计 | 应收、实收、计量读数的概念来源？ | `04_billing_er.html`、V1 的 bill_item/payment/meter_reading | 1000 | 收费子 ER | 事实分层和多值属性拆分 |
| 3.3.1 租赁、车证和通知的关系实体 | 为什么授权/参与/收件/申请不能省略？ | `05_vehicle_parking_permit_er.html`、`06_resident_service_notification_er.html`、V4/V8 | 900 | 2 张局部小图 | N:M 与独立状态历史 |

## 第 4 章 逻辑模型与物理数据库设计（6,500 字）

| 三级节 | 要回答的问题 | 真实素材 | 字数 | 建议图/表 | 数据库关联 |
|---|---|---|---:|---|---|
| 4.1.1 概念 ER 到关系模式 | 主体、弱实体、联系表如何转换？ | `docs/database/RELATION_SCHEMA_V1.md` 与最终 dump | 1000 | 关系模式总表 | 概念→逻辑映射 |
| 4.1.2 范式与冗余控制 | 费用类型/标准、账单/明细、文件元数据拆分是否合理？ | `NORMALIZATION_REPORT_V1.md`、V1/V3 DDL | 1000 | 函数依赖/范式表 | 3NF 讨论需有真实键 |
| 4.2.1 空间与人房主表 | PK、FK、复合 UK、当前状态与历史关系如何实现？ | V1、V3 与 dump 中 7+3 张表 | 1200 | 精选 DDL 表 | 关系完整性 |
| 4.2.2 收费、租赁、通知与车证表 | 关键业务事实、状态字段与关联键为何如此设计？ | V1/V4/V7/V8 | 1200 | 表域/主外键矩阵 | 关系与生命周期设计 |
| 4.3.1 约束与异常数据 | MySQL 为业务挡住哪些无效写入？ | `03_constraint_negative_cases.sql`、`07_negative_cases.txt`、V8 CHECK | 1100 | 规则→约束→NEG 编号表 | PK/FK/UK/CHECK/RESTRICT |
| 4.3.2 索引与访问路径 | 哪些索引出于键约束、哪些为查询优化？ | V6、`STAGE_G_INDEX_AUDIT.md`、`PPT_DATABASE_PERFORMANCE_AUDIT.md` | 1000 | 代表索引/EXPLAIN 小图 | 组合索引与代价 |

## 第 5 章 数据库演进、视图与多小区安全（5,500 字）

| 三级节 | 要回答的问题 | 真实素材 | 字数 | 建议图/表 | 数据库关联 |
|---|---|---|---:|---|---|
| 5.1.1 V1–V3 初建到身份/资产扩展 | 25 表如何逐步新增身份、文件、审计？ | V1–V3、Flyway history | 1000 | V1–V3 版本时间轴 | 版本化 DDL |
| 5.1.2 V4–V10 的功能性结构演进 | 范围/通知/索引/报事/车证/户型变体为何各占一版？ | V4–V10 | 1100 | V4–V10 变更矩阵 | 数据库演进工程化；如实指出 V8 示例数据 |
| 5.2.1 财务两层 VIEW | 应收、成功实收、欠费与月聚合如何算？ | dump 末尾两个 Final VIEW、`BillMapper.xml` | 1200 | VIEW 依赖图+公式 | 复用一致财务口径 |
| 5.3.1 账号→权限→小区范围 | 角色与社区数据作用域如何实体化？ | V3/V4、`AccessControlMapper.xml`、`DataScopeServiceImpl.java` | 1100 | 权限关系链 | N:M scope 与授权 |
| 5.3.2 SQL 级过滤与安全验证 | global、多个 communityIds、空授权如何落在 SELECT？ | `PropertyMapper.xml`、`StatisticsMapper.xml`、Stage H-R1 报告 | 1100 | 三种 SQL 条件对照 | 行级作用域和防越权 |

## 第 6 章 系统实现与数据库调用链（5,500 字）

| 三级节 | 要回答的问题 | 真实素材 | 字数 | 建议图/表 | 数据库关联 |
|---|---|---|---:|---|---|
| 6.1.1 分层技术与统一 HTTP 契约 | Vue→Controller→Service→Mapper 接口/XML 的职责是什么？ | `frontend/src/api/http.js`、Spring 包结构、`application.yml` | 800 | 调用架构图 | 以 SQL 为最后事实源 |
| 6.2.1 双房产分页的实际差异 | 旧 `/api/v1` 和管理 `/api/admin` 各怎样 COUNT、LIMIT/OFFSET、DataScope？ | 两套 Controller/Service/Mapper XML、`PaginationBar.vue` | 1300 | 并排链路与 SQL | 动态 SQL、分页一致性 |
| 6.2.2 居住与租赁链 | 查询当前关系、办理入住和挂牌如何触表？ | `ResidentMapper.xml`、`ResidenceManagementServiceImpl`、`RentalMapper.xml` | 1000 | 读写表清单 | 时态关系与事务 |
| 6.2.3 账务与 Dashboard 链 | 每张卡/图从哪条 SQL 来？ | `DashboardView.vue`、`DashboardMapper.xml`、`StatisticsMapper.xml` | 1100 | 图表→API→SQL 映射 | VIEW 与聚合验证 |
| 6.3.1 MinIO 文件/户型图 | 文件二进制与关系元数据怎样互相定位？ | `FloorPlanService.java`、`FileObjectMapper.xml`、V3/V9/V10 | 700 | 文件对象关系图 | DB 元数据与对象一致性 |
| 6.3.2 通知与车证事务 | 多收件人与审核发证如何保持状态一致？ | `NotificationServiceImpl`、`ParkingPermitServiceImpl`、XML | 600 | 事务步骤图 | 多表原子性与锁 |

## 第 7 章 测试、性能与可靠性评价（4,000 字）

| 三级节 | 要回答的问题 | 真实素材 | 字数 | 建议图/表 | 数据库关联 |
|---|---|---|---:|---|---|
| 7.1.1 初始结构与种子断言 | V1 的 14 PASS 到底验证了什么？ | `05_seed_assertions.sql`、`09_seed_assertions.txt` | 900 | A01–A14 精选表 | 实例满足关系模型 |
| 7.1.2 16 项负向约束 | 哪些非法写入确由 DB 拒绝？ | `03_constraint_negative_cases.sql`、`07_negative_cases.txt` | 900 | NEG 规则/错误码表 | 数据库层防错 |
| 7.2.1 事务与冲突控制 | version、FOR UPDATE、真实失败回滚有何证据？ | `PropertyUnitMapper.xml`、`ParkingPermitMapper.xml`、`PPT_TRANSACTION_ROLLBACK_EVIDENCE.md` | 1100 | 条件更新与回滚时间线 | 并发一致性；不夸大跨 MinIO 原子性 |
| 7.3.1 查询计划、历史负载与限制 | 索引是否支持现有规模？负载结果能证明什么？ | `PPT_DATABASE_PERFORMANCE_AUDIT.md`、2026-09-02 两 JSON 报告 | 1100 | EXPLAIN/负载结果小表 | 性能证据须注明场景/时间 |

## 第 8 章 部署恢复、问题与改进方向（2,500 字）

| 三级节 | 要回答的问题 | 真实素材 | 字数 | 建议图/表 | 数据库关联 |
|---|---|---|---:|---|---|
| 8.1.1 Docker 数据与容器生命周期 | MySQL/MinIO 怎样持久化，Redis 为何不迁移？ | `compose.yaml`、`DOCKER_STAGE1_RUNBOOK.md` | 850 | 容器/volume 结构图 | 备份恢复、数据持久化 |
| 8.1.2 恢复验证与可观测追踪 | 10 迁移、2 VIEW、12 对象、SQL 链路如何历史验证？ | Stage1/2 Runbook、观测 Runbook、既有日志 | 850 | 恢复核对表与脱敏 Flow 截图 | 可重复验证；仍需注明历史日期 |
| 8.2.1 已知不足与可行后续工作 | 迁移来源缺口、在线缴费链缺失、跨存储事务问题如何客观评价？ | 本次考古、性能/文件审计 | 800 | 已实现/未实现/待证实三栏 | 设计边界和改进优先级 |

## 第 9 章 结论（700 字）

| 三级节 | 要回答的问题 | 真实素材 | 字数 | 建议图/表 | 数据库关联 |
|---|---|---|---:|---|---|
| 9.1.1 成果归纳与限度 | 关系设计、约束、数据作用域、视图和版本化的实际贡献是什么；结论不能超出哪些证据？ | 前八章表/图/测试索引 | 700 | 不必再放大图 | 数据库课程核心收束 |

**字数校验：** 2,400 + 3,200 + 4,700 + 6,500 + 5,500 + 5,500 + 4,000 + 2,500 + 700 = **35,000 字**。数据库概念/逻辑/物理/约束/查询/事务/验证约占大头，Docker/观测主要留第八章与附录。建议附录另放完整迁移目录、简化表字典、接口表、脱敏 SQL 与截图索引；不要用附录填充正文长度。
