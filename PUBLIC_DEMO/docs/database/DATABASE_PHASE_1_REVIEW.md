# DATABASE_PHASE_1_REVIEW · 数据库阶段审查包

状态：**PASS（等待人工审查）**  
最终目录回归时间：2026-08-20 16:17:52 +08:00  
实测环境：MySQL Community Server 8.0.43 / Flyway Maven Plugin 13.1.0 / JDK 21 / Maven 3.9.8

## 1. 本轮目标与边界

已完成 Demo Phase 1：原 Stage 01 数据库逻辑设计 + 原 Stage 02 Database V1。

本轮没有创建 Spring Boot、Vue、Controller、Service、Mapper、Axios、Redis、MinIO、地图、Grafana、Prometheus 或全系统 Docker Compose。数据库通过人工审查前，不进入下一阶段。

## 2. 项目结构

```text
smart-property-platform-codex-spec-v1/
├── backend/
│   └── README.md                         # 下一阶段边界；无后端代码
├── frontend/
│   └── README.md                         # 下一阶段边界；无前端代码
├── deploy/
│   └── README.md                         # 后续部署边界
├── database/
│   ├── DATABASE_V1_GUIDE.md              # 建库、验证、回滚指南
│   ├── migrations/                       # 唯一 migration 来源
│   │   ├── V1__init_schema.sql
│   │   └── V2__seed_reference_data.sql
│   ├── sample-data/
│   │   └── 01_demo_data.sql
│   ├── tests/
│   │   ├── 01_schema_check.sql
│   │   ├── 02_positive_cases.sql
│   │   ├── 03_constraint_negative_cases.sql
│   │   ├── 04_core_join_queries.sql
│   │   └── 05_seed_assertions.sql
│   ├── flyway/
│   │   ├── pom.xml
│   │   └── flyway.conf.example
│   ├── scripts/
│   │   └── verify-database-v1.ps1
│   └── evidence/                         # 本次真实执行日志
│       ├── 00_mysql_initialize.txt
│       ├── 01_database_bootstrap.txt
│       ├── 02_flyway_migrate.txt
│       ├── 03_flyway_info.txt
│       ├── 04_seed_import.txt
│       ├── 05_schema_check.txt
│       ├── 06_positive_cases.txt
│       ├── 07_negative_cases.txt
│       ├── 08_core_join_queries.txt
│       ├── 09_seed_assertions.txt
│       └── VERIFICATION_SUMMARY.txt
└── docs/
    └── database/
        ├── RELATION_SCHEMA_V1.md
        ├── NORMALIZATION_REPORT_V1.md
        ├── BACKEND_INTEGRATION_HANDOFF.md
        └── DATABASE_PHASE_1_REVIEW.md
```

规范包原有 README、MASTER_SPEC、ER_CONCEPT_V1 等文件没有被修改。

## 3. 最终关系模式（25 张业务表）

1. `community`：小区。
2. `building`：小区内楼栋。
3. `building_unit`：楼栋内单元。
4. `layout_template`：整套房户型模板，包含有限 `layout_type` ENUM。
5. `property_unit`：最小产权/居住/出租/收费房产单元。
6. `person`：现实人员。
7. `ownership`：人员与房产的时态产权关系。
8. `residence`：人员与房产的时态实际居住关系。
9. `agency`：中介公司。
10. `authorization`：Person + PropertyUnit + Agency 三元授权。
11. `rental_listing`：完整房产的一次挂牌生命周期。
12. `lease_contract`：挂牌成交形成的合同。
13. `contract_participation`：人员与合同的 M:N 参与关系。
14. `fee_type`：收费项目字典。
15. `fee_standard`：小区与收费项目的时态标准。
16. `meter`：房产计量表。
17. `meter_reading`：累计抄表记录。
18. `bill`：房产月度账单头。
19. `bill_item`：依赖 Bill 的账单项目弱实体。
20. `payment`：人员对账单的支付事实。
21. `parking_space`：小区车位资产。
22. `vehicle`：人员登记车辆。
23. `parking_right`：人员与车位的时态权利。
24. `facility_type`：家具/设施类型。
25. `facility_placement`：整套 PropertyUnit 户型图二维覆盖层。

完整中英文字段、NULL、候选键、函数依赖、删除策略和典型查询见 `RELATION_SCHEMA_V1.md`。

## 4. ER → Relation Mapping 审查重点

- Ownership 和 Residence 分成两张表；测试数据中张三拥有中关村学苑 101 与海淀春景 101，却实际住在海淀春景 202。
- Authorization 包含 person_id + property_unit_id + agency_id；AGENCY Listing 使用 `(authorization_id,property_unit_id)` 复合 FK，数据库可拒绝“授权房产与挂牌房产不一致”。
- PropertyUnit、RentalListing、LeaseContract 分别建表；同一房产可有多次 Listing，而每次 Listing 最多一个合同。
- ContractParticipation 主键含合同、人员、角色；Seed 合同同时有两名 LANDLORD 与两名 TENANT。
- BillItem 主键 `(bill_id,item_no)`，不能脱离 Bill；金额由 quantity × unit_price 生成。
- ParkingRight 连接 Person 与 ParkingSpace；Vehicle 不含 parking_space_id，换车不改变车位权。
- LayoutTemplate 和 PropertyUnit 的图均表示整套房；FacilityPlacement 直接属于 PropertyUnit，没有 Room。

## 5. 范式结论

全部 V1 关系满足至少 3NF：

- 1NF：不使用人员角色列表、房产列表、设施 CSV 或合同参与人数组。
- 2NF：Ownership、Residence、Authorization、ContractParticipation、ParkingRight、BillItem 的非键属性均依赖完整业务复合键。
- 3NF：PropertyUnit 不重复小区/楼栋名称；BillItem 不重复费种名称或可由 FeeStandard 推导的 fee_type_id；关系表不重复参与实体描述属性。

有意识保存的历史快照只有合同金额和账单项出账单价，语义上是形成时事实，不随当前 Listing/FeeStandard 改变。详细 FD 见 `NORMALIZATION_REPORT_V1.md`。

## 6. 数据库对象实测

来自 `05_schema_check.txt`：

- 25 张应用表；另有 Flyway 自身 `flyway_schema_history`。
- 25 个应用主键。
- 36 个外键约束。
- 29 个 UNIQUE 约束/唯一二级索引。
- 66 个 CHECK 约束。
- 22 个非唯一基础二级索引。

关键约束：同父级编号唯一、完整空间 FK、产权份额/日期、三元授权引用、Listing 模式互斥、Listing→Contract 0..1、BillItem 弱键、设施坐标/移动规则。

## 7. Migration 与单一可信来源

`database/migrations/` 是唯一来源：

- V1：创建 25 张表及约束。
- V2：写入稳定户型枚举模板、收费项目、设施类型等参考数据。

Flyway 实测输出：

```text
Schema version: 2
V1 init schema          Success
V2 seed reference data Success
BUILD SUCCESS
```

Demo 数据不随生产迁移自动导入，单独放在 `database/sample-data/01_demo_data.sql`。

## 8. Seed 覆盖

- 2 个小区；每个 2 栋楼；每栋 2 个单元；共 24 套 PropertyUnit。
- 10 个有限户型模板，涵盖开间、一居室、一室一厅、两居室、两室一厅、两室两厅、三室一厅、三室两厅、四室两厅、其他。
- 6 名 Person；张三跨两个小区拥有房产，且居住在另一套非本人产权房。
- 3 条三元 Authorization；4 个 Listing；1 个多人 LeaseContract。
- 5 个 FeeType、10 个 FeeStandard、4 个 Meter、8 个 MeterReading、3 个 Bill、8 个 BillItem、2 个 Payment。
- 6 个 ParkingSpace、3 个 Vehicle、3 个 ParkingRight。
- 7 个 FacilityType、5 个 FacilityPlacement；包含空调 LIMITED 锚点/半径和热水器 FIXED 点。

## 9. 正向测试

`02_positive_cases.sql` 在事务中实际创建并回滚：

```text
正向测试小区 → 1号测试楼 → 1单元 → 102
```

完整 JOIN 正确返回地址层级、户型和面积。随后同一测试人员拥有新建 102，但实际居住 Seed 的另一房产，结果为 PASS，证明两种关系可独立表达。

## 10. 负向测试

MySQL 实际拒绝 16 个非法写入：

- ERROR 1062 × 4：重复楼栋号、重复单元号、重复房号、同 Listing 第二份合同。
- ERROR 1452 × 7：三个非法空间父 ID、无效授权人员、孤立 BillItem、无效 ParkingRight 人员、授权与 Listing 房产不匹配。
- ERROR 3819 × 5：非法产权份额、产权/居住/授权反向日期、LIMITED 设施缺少锚点与半径。

日志见 `07_negative_cases.txt`；测试用 `--force` 继续执行所有预期失败语句，并在事务末尾回滚，避免污染 Seed。

## 11. 查询验证

`08_core_join_queries.txt` 保存 8 组真实结果：

- 房产完整路径：`中关村学苑小区 / 1号楼 / 1单元 / 101`。
- 张三产权：分别位于两个小区，`COUNT(DISTINCT community_id)=2`。
- 张三实际居住：`海淀春景家园 / 1号楼 / 1单元 / 202`。
- 账单应收/实收：416.60/200.00、455.40/455.40、209.00/0.00。
- 窗口函数返回每块水电表最新读数。
- 有效中介授权返回业主、房产、中介及权限。
- 多方合同聚合返回 `LANDLORD=李四、王芳`，`TENANT=张三、陈晨`。

`09_seed_assertions.txt` 的 14 项断言全部 PASS，包括“不存在 Room/Occupancy 表”。

## 12. 实际执行命令

主验证入口：

```powershell
.\database\scripts\verify-database-v1.ps1
```

脚本内部实际执行：

- MySQL 8 `mysqld --initialize-insecure` 创建隔离临时实例；
- Flyway `flyway:migrate`、`flyway:info`；
- MySQL 客户端依次导入 Seed 和五个测试脚本；
- MySQL `mysqladmin shutdown` 关闭隔离实例；
- 验证临时路径前缀后清理 runtime 数据。

隔离端口为 33306，不修改机器已有的 3306 MySQL 数据库。

## 13. 失败、根因与最小修复

1. 首次独立 mysqld 启动失败：Start-Process 对含空格 basedir 参数拆分。只修正参数引号，原用例通过。
2. Maven/Flyway 首次调用未获得 goal：Windows 批处理把 JDBC URL 的 `&` 当控制符。改用 Flyway 环境变量传参，迁移通过。
3. 负向 SQL 首次只执行第一条：`--execute=SOURCE` 与当前客户端组合未继续 source。改为通过标准输入传入 SQL，16 条全部执行。
4. 错误计数首次把测试说明里的 “expect ERROR” 一并计入。只把正则锚定到实际 `^ERROR` 行，计数与日志一致。
5. MySQL 报 TINYINT(1) 显示宽度弃用警告。改为 `TINYINT UNSIGNED + CHECK (0,1)`，最终 Flyway 日志无警告。

所有修复均按“复现 → 定位层 → 根因 → 最小修复 → 原失败用例 → 全量回归”执行。

## 14. 已知限制

- 跨行时态重叠、产权份额合计、支付合计、标准/读数因果一致等需后续 Service 事务处理；单行 CHECK 无法完整表达。
- 地理位置 V1 使用经纬度 DECIMAL，尚未增加 POINT/SPATIAL INDEX；为后续空间优化实验保留基线。
- 未做高级复合/覆盖/全文索引、视图、触发器、存储过程和查询优化实验；它们属于后续课程阶段。
- Demo 使用对象键占位，不接 MinIO；对象本身未创建。
- 未建立账号/RBAC；Person 只表达业务人员。

## 15. 第二阶段 Handoff

详细边界见 `BACKEND_INTEGRATION_HANDOFF.md`。核心要求：

- 继续使用唯一 migration 来源，不维护两套 DDL。
- 聚合与时态关系由 Service 管理；事务放在 Service 层。
- 数据库约束错误转换为中文业务错误，不暴露 SQL。
- 先完成 Community→Building→BuildingUnit→PropertyUnit 真实纵向链。
- 未收到人工 “Database Phase 1 PASS” 前，不开始后端/前端。

## 16. 回滚与重建

- 隔离验证脚本会自动关闭并清理它自己创建的临时数据库实例。
- 已有 MySQL 测试库需要重建时，只删除明确命名的测试 schema，再从空库执行 Flyway；不要对生产库使用 DROP。
- 已执行 migration 不允许原地修改；未来修复追加 V3、V4。
- 本轮只新增文件，没有删除或覆盖规范包原文。

## 17. 验收清单

| 项目 | 结果 |
|---|---|
| 冻结实体全部映射 | PASS |
| Ownership / Residence 分离 | PASS |
| Authorization 三元联系 | PASS |
| BillItem 弱实体 | PASS |
| ParkingRight 不绑定 Vehicle | PASS |
| 无 Room / Occupancy / AgencyAgent | PASS |
| 函数依赖与 1NF/2NF/3NF | PASS |
| MySQL 8 空库 Flyway migration | PASS |
| 25 张表与约束可验证 | PASS |
| 完整 Seed | PASS |
| 正向 SQL | PASS |
| 16 个负向约束用例 | PASS |
| 核心 JOIN/窗口函数查询 | PASS |
| 可重复一键验证 | PASS |
| 后端集成交接 | PASS |
| 未越界进入 Phase 2 | PASS |

**Database Phase 1 实施与自动验证完成；现在停止，等待人工审查。**
