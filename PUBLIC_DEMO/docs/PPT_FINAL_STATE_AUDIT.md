# PPT Final State Evidence Audit

> 审计对象：`C:\Projects\数据库课程设计\多小区智慧物业管理系统_MERGED_V1`  
> 审计性质：最终状态只读证据审计；不启动应用、不执行写入、不修改源码/配置/数据库。  
> 证据优先级：运行中 MySQL `community_db` > 当前源码与 Mapper XML > 已执行 Flyway 迁移 > 既有文档。  
> PPT 产出状态：本文件只为后续答辩制作提供事实底稿；本轮**未创建、未修改 PPTX**。

---

## 1. Executive Summary / 答辩可主张结论

该项目当前不是一个空壳原型，而是一套已经具有真实关系数据库、真实业务数据、角色与数据范围控制、文件对象存储以及空间地图能力的多小区物业管理系统。后端采用 Spring Boot + MyBatis + MySQL，前端采用 Vue 3 + Vite + Element Plus + ECharts；核心业务数据位于同一个 `community_db`。

本次只读核验的关键结论如下：

| 结论 | 已核实证据 |
|---|---|
| 数据库演进稳定 | Flyway 已成功执行 V1–V10；最高版本 V10；失败迁移数为 0。 |
| 数据库规模满足课堂数据量门槛 | 47 张基础表、2 个视图；例如房产 528、人 606、居住关系 597、账单明细 18,273、抄表记录 15,840。 |
| 多小区资产层级真实存在 | 4 个小区、14 栋楼、40 个单元、528 套房产。 |
| 收费链路具有真实数据 | 2026 年 1–7 月每月均有 528 张账单；共 3,696 张账单、18,273 条账单明细、3,324 条成功缴费。 |
| 统计不是硬编码 | Dashboard、月度/年度统计由 MyBatis SQL 从真实账单、缴费、房产、视图聚合得出。 |
| 权限与数据范围不是只有前端隐藏 | Spring Security/JWT、`AuthorizationService`、`DataScopeService` 与 SQL 范围条件共同参与访问控制。 |
| 文件存储已落地 | MinIO 保存户型图二进制对象；MySQL 保存 `file_object` 元数据和关联关系；V10 已有 12 个面积精确的户型图变体映射。 |
| 地图有真实后端接口和坐标数据 | 4 个小区、14 栋 ACTIVE 楼均有坐标；Map Scene、缺失坐标补全、POI 搜索/反查、空间边界接口均存在。 |

答辩时建议将系统定位为：**“以多小区空间资产为主线，将人员入住、收费计量、通知、租赁、停车和空间服务接入同一 MySQL 关系模型，并以权限和数据范围控制保障多角色使用。”**

### 必须诚实保留的边界

- 当前数据库没有触发器，也没有存储过程；不能在答辩中宣称已使用这两类对象。
- `spatial_boundary`、`facility_placement`、`parking_space`、`parking_right`、`lease_contract`、`contract_participation`、`account_agency_role` 当前数据量为 0；相应“完整业务数据演示”不应作为主流程。
- Redis 当前是可选的启动/可用性检测与 BYPASS 基础设施，**不是已落地的业务缓存**。
- 本报告没有重新发起高德联网请求，也没有做性能压测；只能说明已有实现与已保存数据，不能替代新的外部服务验收或性能基准。

---

## 2. Project & Database Final Status / 最终工程与数据库状态

### 2.1 Flyway

`flyway_schema_history` 运行时查询结果：

| 安装序号 | 版本 | 迁移说明 | success |
|---:|---:|---|---:|
| 1 | 1 | init schema | 1 |
| 2 | 2 | seed reference data | 1 |
| 3 | 3 | phase2a security assets and files | 1 |
| 4 | 4 | stage f access scope boundary notification | 1 |
| 5 | 5 | notification recipient charge payload | 1 |
| 6 | 6 | stage g bill period status index | 1 |
| 7 | 7 | stage h residence meter and incident | 1 |
| 8 | 8 | parking permit management | 1 |
| 9 | 9 | layout template floor plans | 1 |
| 10 | 10 | layout template floor plan variants | 1 |

结论：最高成功版本为 **V10**；失败迁移数为 **0**；未发现“基线版本”记录。迁移源码位于 `database/migrations/V1__init_schema.sql` 至 `V10__layout_template_floor_plan_variants.sql`，但本审计以当前数据库实际对象为准。

### 2.2 数据库对象清单

| 类别 | 实际数量 | 说明 |
|---|---:|---|
| 基础表 | 47 | 包含 `flyway_schema_history` 1 张迁移元数据表。 |
| 业务及业务辅助表 | 38 | 将迁移元数据表和 8 张账户/权限基础表剥离后的数量；其中含文件、空间等业务辅助表。 |
| 账户/权限基础表 | 8 | `app_account`、`app_role`、`app_permission`、`role_permission`、`account_global_role`、`account_community_scope`、`account_agency_role`、`refresh_token_session`。 |
| 文件/空间辅助表 | 7 | `file_object`、`property_floor_plan`、`layout_template_floor_plan`、`layout_template_floor_plan_variant`、`spatial_boundary`、`facility_type`、`facility_placement`；它们同时属于业务数据，不与上一行简单相加。 |
| 视图 | 2 | `v_bill_financial_summary`、`v_monthly_collection_summary`。 |
| 外键 | 77 | 由运行中 `information_schema.table_constraints` 查询得出。 |
| 唯一约束 | 50 | 由运行中 `information_schema.table_constraints` 查询得出。 |
| CHECK 约束 | 108 | 由运行中 `information_schema.table_constraints` 查询得出。 |
| 索引 | 152 | `information_schema.statistics` 中按“表名 + 索引名”去重统计，含主键/唯一索引。 |
| Trigger | 0 | 当前没有实际触发器。 |
| Stored Procedure / Function | 0 | 当前没有实际例程；不应宣称使用了存储过程。 |

### 2.3 重要视图

1. `v_bill_financial_summary`：以 `bill` 为中心，左连接账单明细聚合应收、成功缴费聚合实收，计算 `outstanding_amount = max(receivable - received, 0)`。
2. `v_monthly_collection_summary`：在上一视图基础上按 `billing_year`、`billing_month` 汇总账单数、应收、实收、欠费和收缴率。

这两个真实视图为账单列表、欠费查询、Dashboard、居民账单和统计接口提供了可解释的收费口径。

---

## 3. Business Domain Inventory / 业务域与表归类

| 业务域 | 主要表 | 核心含义 |
|---|---|---|
| 空间资产 | `community`、`building`、`building_unit`、`property_unit`、`layout_template`、`asset_change_audit` | 小区—楼栋—单元—房产的层级资产模型及变更审计。 |
| 人员、产权、居住 | `person`、`ownership`、`residence`、`resident_incident` | 人与房的产权关系、居住关系、居民异常事项。 |
| 收费、计量、账单 | `fee_type`、`fee_standard`、`meter`、`meter_reading`、`bill`、`bill_item`、`payment` | 收费规则、表具、抄表、账单、账单明细和实缴记录。 |
| 租赁与中介 | `rental_listing`、`authorization`、`agency`、`agency_staff`、`lease_contract`、`contract_participation` | 房产挂牌、委托/授权、中介主体、合同预留结构。 |
| 停车与车证 | `vehicle`、`parking_space`、`parking_right`、`residence_parking_eligibility`、`parking_permit_application`、`parking_permit` | 车辆、停车权益、资格、居民申请、管理员审核和车证。 |
| 通知 | `notification`、`notification_recipient` | 通知正文、接收人、TO/CC、已读状态和收费载荷。 |
| 账户、权限、数据范围 | `app_account`、`app_role`、`app_permission`、`role_permission`、`account_global_role`、`account_community_scope`、`account_agency_role`、`refresh_token_session` | 账户、角色、权限、全局角色、小区范围、机构角色及刷新会话。 |
| 文件与户型图 | `file_object`、`property_floor_plan`、`layout_template_floor_plan`、`layout_template_floor_plan_variant` | MinIO 对象的元数据和“房产优先—面积变体—模板默认”关联。 |
| 空间地图 | `spatial_boundary`、`facility_type`、`facility_placement` | 人工校准的边界与设施位置的可扩展模型。 |
| 系统演进 | `flyway_schema_history` | 数据库版本演进元数据，不是业务表。 |

---

## 4. Recommended Core ER Diagram / 推荐答辩核心 ER 图

PPT 不宜把 47 表全部堆进一张图。建议主 ER 图保留以下 15 个实体；其它表在“扩展能力”页展开。

```text
Community 1 ──< Building 1 ──< BuildingUnit 1 ──< PropertyUnit >── 1 LayoutTemplate
                                                     │
                      ┌──────────────────────────────┼───────────────────────────┐
                      │                              │                           │
Person >──< Ownership │                        Person >──< Residence              │
                      │                              │                           │
                      ├──< Meter 1 ──< MeterReading  │                           │
                      ├──< Bill 1 ──< BillItem >── 1 FeeStandard >── 1 FeeType    │
                      │       │                                                           
                      │       └──< Payment                                                     
                      ├──< RentalListing >── 0..1 Authorization                                
                      └──< ParkingPermitApplication 1 ── 0..1 ParkingPermit                     

Notification 1 ──< NotificationRecipient >── 1 Person
FileObject 1 ──< LayoutTemplateFloorPlanVariant >── 1 LayoutTemplate
```

### 产权关系与居住关系为什么必须分开

`ownership` 与 `residence` 都连接 `person` 和 `property_unit`，但表达的是两类不同事实，不能合成一张表：

- `ownership` 表示**产权/业主关系**，处理共有产权与产权起止历史；当前有效关系以 `end_date is null` 判定。
- `residence` 表示**入住/居住关系**，处理责任人/成员、亲属关系、入住和搬出、租期预计结束等业务；同一房产可以有多人居住，且居住人不必是业主。

分离后可避免“租客被误当作产权人”“家庭成员丢失责任人关系”等更新异常，并支持居民门户、停车资格、账单责任和租赁业务分别取数。

---

## 5. Simplified Relation Schema / 简化关系模式与范式证据

以下适合在 PPT 的“逻辑结构设计”页展示。`PK` 为主键，`FK` 为外键，`UQ` 为唯一约束。

```text
COMMUNITY(community_id PK, community_code UQ, community_name, address, longitude, latitude, ...)
BUILDING(building_id PK, community_id FK→COMMUNITY, building_no, ..., UQ(community_id, building_no))
BUILDING_UNIT(building_unit_id PK, building_id FK→BUILDING, unit_no, ..., UQ(building_id, unit_no))
PROPERTY_UNIT(property_unit_id PK, building_unit_id FK→BUILDING_UNIT,
              layout_template_id FK→LAYOUT_TEMPLATE, property_no, gross_area, usable_area, status, occupancy_status,
              UQ(building_unit_id, property_no))

PERSON(person_id PK, full_name, identity_no, phone, ...)
OWNERSHIP(ownership_id PK, person_id FK→PERSON, property_unit_id FK→PROPERTY_UNIT,
          start_date, end_date, ..., UQ(person_id, property_unit_id, start_date))
RESIDENCE(residence_id PK, person_id FK→PERSON, property_unit_id FK→PROPERTY_UNIT,
          residence_role, relation_to_responsible, start_date, end_date, expected_end_date,
          UQ(person_id, property_unit_id, start_date))

FEE_TYPE(fee_type_id PK, fee_code UQ, fee_name, ...)
FEE_STANDARD(fee_standard_id PK, fee_type_id FK→FEE_TYPE, ..., effective_start, effective_end)
METER(meter_id PK, property_unit_id FK→PROPERTY_UNIT, fee_type_id FK→FEE_TYPE, ...)
METER_READING(meter_reading_id PK, meter_id FK→METER, recorder_person_id FK→PERSON, reading_at, reading_value,
              UQ(meter_id, reading_at))
BILL(bill_id PK, property_unit_id FK→PROPERTY_UNIT, bill_no UQ, billing_year, billing_month, status,
     UQ(property_unit_id, billing_year, billing_month))
BILL_ITEM(bill_id PK/FK→BILL, item_no PK, fee_standard_id FK→FEE_STANDARD,
          previous_reading_id FK→METER_READING, current_reading_id FK→METER_READING, amount)
PAYMENT(payment_id PK, bill_id FK→BILL, payer_person_id FK→PERSON, payment_no UQ,
        external_transaction_no UQ, amount, status, paid_at)

NOTIFICATION(notification_id PK, sender_account_id FK→APP_ACCOUNT, ...)
NOTIFICATION_RECIPIENT(notification_recipient_id PK, notification_id FK→NOTIFICATION,
                       person_id FK→PERSON, recipient_type, read_at, UQ(notification_id, person_id))
```

### 规范化与完整性证据

- **1NF**：主要业务字段为原子值；账单项目拆分为 `bill_item`，而非将多条费用写进账单字符串列。
- **2NF**：`bill_item` 使用 `(bill_id, item_no)` 复合主键，费用/读数等依赖完整的明细标识；桥接表将多对多关系独立出来。
- **3NF（核心业务表）**：小区、楼栋、单元、房产、人、收费类型、收费标准均独立存储，非键事实不依赖于可传递的名称字段；报表视图只派生聚合值，不反向作为事实源。
- **关系完整性**：当前库有 77 个外键。例如 `property_unit → building_unit/layout_template`、`bill → property_unit`、`bill_item → bill/fee_standard/meter_reading`、`payment → bill/person`、`notification_recipient → notification/person`。
- **去重完整性**：例如 `uk_bill_property_period`、`uk_bill_no`、`uk_meter_reading_time`、`uk_property_unit_parent_no`、`uk_layout_template_floor_plan_variant_area`。
- **业务 CHECK 示例**：账单月份 1–12、缴费金额大于 0、房产使用面积不超过建筑面积、居住责任人必须为 SELF、车证报价总额必须等于月单价 × 月数、租赁模式的人员/授权二选一约束。

结论：核心关系模式符合面向事务型物业系统的第三范式设计意图。本结论是对核心表结构的静态审计，不等同于对 47 张表的形式化数学证明。

---

## 6. Actual Data Volume & Classroom Threshold / 真实数据量与课程门槛

### 6.1 关键事实表数据量

| 领域 | 表 | 行数 |
|---|---|---:|
| 空间资产 | `community` / `building` / `building_unit` / `property_unit` | 4 / 14 / 40 / 528 |
| 户型 | `layout_template` | 10 |
| 人员与关系 | `person` / `ownership` / `residence` | 606 / 528 / 597 |
| 收费基础 | `fee_type` / `fee_standard` | 10 / 28 |
| 表具与抄表 | `meter` / `meter_reading` | 1,584 / 15,840 |
| 账单与缴费 | `bill` / `bill_item` / `payment` | 3,696 / 18,273 / 3,324 |
| 租赁 | `rental_listing` / `authorization` / `agency` / `agency_staff` | 80 / 64 / 3 / 10 |
| 停车车证 | `vehicle` / `parking_permit_application` / `parking_permit` | 5 / 5 / 3 |
| 通知 | `notification` / `notification_recipient` | 7 / 507 |
| 居民异常 | `resident_incident` | 2 |
| 账户权限 | `app_account` / `app_role` / `app_permission` / `role_permission` | 59 / 5 / 12 / 24 |
| 账户范围 | `account_global_role` / `account_community_scope` / `refresh_token_session` | 59 / 5 / 198 |
| 文件与户型图 | `file_object` / `layout_template_floor_plan` / `layout_template_floor_plan_variant` | 12 / 4 / 12 |

补充现状：当前有效产权关系 528 条，当前有效居住关系 596 条；2026 年 1–7 月每月各有 528 张账单。

### 6.2 对照课程要求

课程要求材料中强调了“实体数据不少于 30、关系数据不少于 200、至少 3 项统计、展示完整业务流程”等答辩重点。当前库的事实数据明显满足前两项：

| 要求 | 直接证据 | 结论 |
|---|---|---|
| 单类实体数据 ≥ 30 | `property_unit=528`、`person=606`、`meter=1,584`、`bill=3,696` | PASS |
| 关系数据 ≥ 200 | `ownership=528`、`residence=597`、`bill_item=18,273`、`meter_reading=15,840`、`notification_recipient=507` | PASS |
| 至少 3 项统计 | Dashboard、年度/月度收费、季度/费用构成/入住概览、月度小区下钻 | PASS（以真实 SQL 为基础） |

### 6.3 不能作为“数据充足”演示的模块

| 表 | 当前行数 | 答辩处理建议 |
|---|---:|---|
| `parking_space` / `parking_right` | 0 / 0 | 只说明架构预留；主演示使用车证申请/审核即可。 |
| `lease_contract` / `contract_participation` | 0 / 0 | 不演示“正式合同签订闭环”。 |
| `account_agency_role` | 0 | 不把中介账号角色绑定说成已有示例数据。 |
| `spatial_boundary` / `facility_placement` | 0 / 0 | 说明可人工维护/扩展，但不要展示为已有边界或设施数据。 |
| `property_floor_plan` | 0 | 不演示用户单房上传覆盖；演示默认/面积变体户型图。 |

---

## 7. Feature, Page & API Evidence / 功能、页面、接口证据

以下状态是“当前代码存在可调用链路 + 当前数据库具备相应事实数据”的审计标签；不等于本轮重新进行全部浏览器回归。

| 模块 | 主要页面/路由 | 主要接口或后端链路 | 状态 | 证据与边界 |
|---|---|---|---|---|
| 登录与会话 | `/login` | `POST /api/v1/auth/login`、`refresh`、`logout`、`me` | COMPLETED | `AuthController`、JWT、刷新会话表；密码采用 BCrypt 编码。 |
| 资产工作台 | `/assets` | 小区/楼栋/单元/房产相关 Controller 与 Service/Mapper | COMPLETED | 4→14→40→528 层级数据，含资产变更审计。 |
| Dashboard | `/dashboard` | `DashboardController`、`DashboardMapper.xml` | COMPLETED | 指标取自房产和 `v_bill_financial_summary`，不是静态数字。 |
| 房产管理 | `/properties` | `GET /api/admin/properties`、详情接口 | COMPLETED | 真实分页、搜索、筛选与数据范围条件；528 房产。 |
| 居民与入住 | `/residents`、`/residents/manage` | `ResidentController`、`ResidenceManagementController` | COMPLETED | 人员搜索/创建、可入住房产、入住、搬出；597 条居住关系。 |
| 居民门户 | `/resident/*` | `/api/resident/me/{summary,residences,properties,bills,notifications,incidents}` | COMPLETED | 以 JWT 中的 `personId` 绑定数据，不按前端传入 personId 取数。 |
| 账单与缴费 | `/bills`、`/fees` | `BillController`、`FeeCenterController`、账单/缴费 Mapper | COMPLETED | 3,696 账单、18,273 明细、3,324 成功缴费，使用财务视图。 |
| 收费统计 | `/statistics/monthly`、`/statistics/annual` | `/api/admin/statistics/*` | COMPLETED | 月度、年度、季度、费用构成、入住概览和下钻 SQL 已实现。 |
| 租赁挂牌 | `/rentals` | `RentalController` | PARTIAL | 挂牌 80 条（50 PUBLISHED、15 RENTED）；正式合同/参与方表为 0。 |
| 物业通知 | `/notifications` | `NotificationController`、`NotificationServiceImpl.send` | COMPLETED | 7 通知、507 接收人；支持分层选择、TO/CC、收费通知载荷。 |
| 车证管理 | `/parking-permits`、`/resident/parking-permits` | 管理端/居民端车证 Controller 与 `ParkingPermitServiceImpl` | COMPLETED（当前样例小） | 5 申请、3 车证；车位/停车权益独立数据尚未填充。 |
| 居民异常 | `/incidents`、居民门户异常入口 | `ResidentPortalController`、`ResidentIncidentAdminController` | COMPLETED（当前样例小） | 已有提交、管理员状态更新、2 条样例。 |
| 空间地图 | `/map` | `/api/admin/map/scene`、坐标补全、POI 搜索/反查、边界接口 | COMPLETED（外部服务依赖） | 4 小区/14 楼坐标已存；边界和设施位置信息尚为空。 |
| 户型图 | 房产详情抽屉 | `FloorPlanController` → `FloorPlanService` → MinIO | COMPLETED | MySQL 元数据 12 条，V10 面积变体 12 条；单房覆盖表暂为 0。 |

---

## 8. End-to-End Workflows / 可展示的完整业务流程

以下流程可直接用于“系统演示”部分。每条均强调页面、Controller、Service、Mapper、数据库表和状态变化，而不只是展示一个界面。

### 流程 A：管理员办理入住 / 搬出（建议主演示之一）

```text
办理入住页面 /residents/manage
  → POST /api/admin/residence-management/move-ins
  → ResidenceManagementController.moveIn
  → ResidenceManagementServiceImpl.moveIn (@Transactional)
  → ResidenceManagementMapper
  → person + residence + property_unit + residence_parking_eligibility
  → 写入入住关系，并更新房产居住/占用状态；搬出时关闭相应 residence
```

展示价值：清晰体现“人员—房产—居住关系”而不是把人名直接放在房产表中；还可说明停车资格随居住资格关联。

### 流程 B：居民申请车证 → 管理员审核 → 生成车证（建议主演示之二）

```text
居民门户 /resident/parking-permits
  → POST 居民车证申请接口
  → ParkingPermitServiceImpl.submitMyApplication (@Transactional)
  → vehicle + residence/ownership 资格校验 + parking_permit_application

管理员 /parking-permits
  → 审核申请
  → ParkingPermitServiceImpl.review (@Transactional)
  → 锁定人员/房产、校验名额与资格
  → 更新 parking_permit_application(PENDING → APPROVED/REJECTED)
  → APPROVED 时写入 parking_permit
```

展示价值：涉及状态机、金额规则、身份校验、事务和外键关联；当前已有 5 条申请、3 张车证可用于查询演示。

### 流程 C：管理员发送物业/收费通知 → 居民接收与已读

```text
通知中心 /notifications
  → POST 管理端通知接口
  → NotificationController
  → NotificationServiceImpl.send (@Transactional)
  → 按小区/楼栋/房产/人员解析接收目标并校验数据范围
  → notification + notification_recipient(TO/CC, payload)
  → 事务提交后执行预览日志；居民门户读取本人通知
```

展示价值：同一通知可向层级对象展开为多个接收人；当前 507 条接收关系足以展示关系数据规模。

### 流程 D：账单 → 明细 → 成功缴费 → 统计更新

```text
物业账单 /bills 或收费中心 /fees
  → BillController / FeeCenterController
  → 对 v_bill_financial_summary 查询
  → bill + bill_item + payment
  → 应收 = 明细金额合计；实收 = SUCCESS 支付合计；欠费 = max(应收 - 实收, 0)
  → Dashboard / 月度统计从相同事实源重新聚合
```

展示价值：账单、明细、支付和统计共用同一财务口径，避免前端单独计算。当前状态分布：2,954 PAID、370 PARTIALLY_PAID、372 OVERDUE。

### 流程 E：房产详情读取面积精确户型图

```text
房产管理 /properties → PropertyDrawer
  → FloorPlanController / FloorPlanService.download
  → 先查 property_floor_plan（单房覆盖）
  → 再查 layout_template_floor_plan_variant（户型 + 建筑面积精确变体）
  → 最后查 layout_template_floor_plan（模板默认）
  → file_object 元数据 → MinIO 对象流 → 前端预览
```

展示价值：体现关系型元数据与对象存储分工，以及 V10 对“同户型不同面积不同图”的精确映射。当前有 12 个变体映射、4 个默认模板映射、0 个单房覆盖映射。

---

## 9. Real Statistics & Recommended Presentation Set / 真实统计与推荐展示组合

### 已有真实统计能力

1. **Dashboard 综合指标**：房产总量、入住数/入住率、应收、实收、欠费、收缴率、欠费排行等。`DashboardMapper.xml` 直接从房产层级和 `v_bill_financial_summary` 计算。
2. **年度/月度收费统计**：`StatisticsMapper.xml` 按年、月聚合账单数、应收、实收、欠费与收缴率；前端年度页面按完整 1–12 月轴展示，未出账月份保持 no-data/null 的语义，而不伪造为 0。
3. **季度与费用构成统计**：按季度聚合收费，并通过 `bill_item → fee_standard → fee_type` 汇总各类费用应收。
4. **入住概览**：按 `occupancy_status` 聚合业主入住、租客入住、空置、待租等状态。
5. **月度下钻**：年度 → 月度 → 小区 → 指定小区明细，接口包括 `/monthly/communities` 与 `/monthly/drilldown`。

### 推荐答辩中展示的三项

1. **年度收费趋势**：展示 2026 年 1–7 月真实数据及 8–12 月“暂无账单数据”的空白时间轴。
2. **月度小区收费下钻**：从一个月份进入各小区应收/实收/欠费，再进入单小区细节，突出多小区管理。
3. **Dashboard 入住率 + 欠费 Top**：同时体现资产状态和财务风险。

统计事实：当前实际账单覆盖 2026 年 1–7 月，每月 528 张；之后月份不存在账单数据，不应以“金额为 0”对外表述。

---

## 10. Technology Architecture / 技术架构与真实使用方式

### 后端

| 技术 | 当前工程证据 | 实际职责 |
|---|---|---|
| Java 21 | `backend/pom.xml` | 后端运行语言版本。 |
| Spring Boot 3.5.16 | `spring-boot-starter-parent` | Web、验证、AOP、Actuator、依赖注入与应用生命周期。 |
| Spring Security + OAuth2 Resource Server | `SecurityConfig` | Stateless JWT 认证、CORS、安全响应。 |
| MyBatis Spring Boot 3.0.5 | POM 与 `resources/mapper/**` | Mapper 接口 + XML 承载复杂 SQL、分页、聚合和范围条件。 |
| MySQL 8 | 运行中 `community_db` | 关系模型、约束、视图、索引和实际业务数据。 |
| Flyway | `flyway-core`、`flyway-mysql` | V1–V10 结构/参考数据演进。 |
| MinIO Java SDK 8.5.7 | `FloorPlanService` 等 | 户型图上传、下载、对象存储可用性和 Bucket 准备。 |

### 前端

| 技术 | 当前工程证据 | 实际职责 |
|---|---|---|
| Vue 3.5.22 | `frontend/package.json` | 管理端、居民端、多角色路由和组件化页面。 |
| Vite 7.1.7 | `npm run dev/build` | 前端开发与构建。 |
| Vue Router 4.5.1 | `frontend/src/router/index.js` | 管理端/居民端路由分流与登录守卫。 |
| Element Plus 2.11.3 | `main.js`、各 View | 表格、分页、表单、抽屉、通知等后台组件。 |
| Axios 1.12.2 | `frontend/src/api/http.js` | HTTP API 调用、响应/错误处理。 |
| ECharts 6.0.0 | 统计页面 | 真实统计数据的图表可视化。 |
| 高德 JS API Loader | `frontend/src/map/amapLoader.js`、`AmapMap.vue` | 正常底图、Marker、InfoWindow、边界绘制与缩放交互。 |

### Redis：准确现状

项目引入 `spring-boot-starter-data-redis`，且 `RedisAvailabilityService` 执行连接/PING 检测；`LocalDevelopmentServices` 可以准备本地 Redis；启动和系统状态会显示 `READY` 或 `BYPASS`。源码注释明确 Redis 不可用时业务走 `CACHE BYPASS`。

**审计结论：Redis 是当前的可选基础设施可用性层，不是已经用于账单/房产/统计业务读写的缓存层。**本 PPT 不应宣称“Redis 已缓存核心业务数据”。

### MinIO：准确现状

MinIO 并非仅在依赖中出现：`StorageAvailabilityService` 检查/创建 Bucket，`FloorPlanService` 使用 `putObject/getObject/removeObject`，模板和面积变体导入服务使用 `putObject`。MySQL 的 `file_object` 保存 bucket、object key、原始文件名、类型、大小、哈希和状态；实际二进制对象由 MinIO 管理。

---

## 11. Permissions, Data Scope & Security / 权限、数据范围与安全

### 11.1 认证与角色

- `SecurityConfig` 采用无状态会话和 JWT Resource Server。
- 登录、刷新、登出、当前用户接口位于 `/api/v1/auth/*`；刷新令牌会话持久化在 `refresh_token_session`。
- `JwtService` 将账户 ID、用户名、角色、tokenVersion，以及需要时的 `personId` 写入访问令牌；`SpringSecurityActorContextProvider` 从认证主体构造 `ActorContext`。
- 密码编码器为 BCrypt（strength 12）；JWT 密钥要求至少 32 字节 Base64 随机数据，配置不会写入审计报告。

### 11.2 数据作用域

`DataScopeServiceImpl` 的实际规则：

```text
SYSTEM_ADMIN → global=true，可查看全量管理范围
其他已认证角色 → 从 account_community_scope 查询可访问的小区 ID
无可访问范围 → SQL 使用 1 = 0，拒绝返回越权数据
```

服务层会在获取小区、楼栋、房产、账单、入住等对象时执行 `requireCommunity / requireBuilding / requireProperty / requireBill / requireResidence`；Mapper XML 对分页与统计查询追加小区 `IN (...)` 条件。例如 `PropertyUnitService`、账单 Mapper、收费中心 Mapper、统计 Mapper、通知目标 Mapper、地图 Mapper 都可以看到数据范围参数或 SQL 条件。

### 11.3 重要说明

`SecurityConfig` 对 `/api/admin/**` 的 HTTP 层基础规则是“已认证”；更细的业务权限由各 Service 通过 `AuthorizationService.requirePermission(...)` 实施。因此 PPT 应描述为：**“HTTP 层先认证，Service 层再检查业务权限与小区数据范围”**，不要简化为“只靠路由隐藏”。

### 11.4 其他安全与可观测性

- 统一 API 信封：`ApiResponseAdvice` 统一成功 JSON；`GlobalExceptionHandler` 统一业务、参数、完整性、权限、上传和未知异常。
- Trace：`RequestTrace`、`X-Trace-Id`、`BusinessAccessLogFilter`、Compact Flow Log 用于关联请求链路；敏感凭据不应输出到普通控制台。
- 数据库层：外键、唯一约束和 CHECK 约束构成最后一道数据完整性保障。

---

## 12. Map & Spatial Capability / 地图与空间能力

### 已经存在的真实能力

| 能力 | 代码/接口证据 | 当前数据状态 |
|---|---|---|
| 地图场景 | `GET /api/admin/map/scene` → `MapSceneServiceImpl` | 4 个小区、14 栋 ACTIVE 楼均已有经纬度。 |
| 缩放分级 | Scene 元数据指定小区最大可见缩放 14、楼栋最小可见缩放 15 | 前端 `AmapMap` 按场景显示。 |
| 缺失坐标补全 | `POST /api/admin/map/locations/sync-missing` → `MapLocationSyncService` | 仅更新缺失坐标，不覆盖已有坐标；会报告失败和重复坐标组。 |
| 周边 POI | `/search/nearby` | 调用 `GeoLocationService.searchNearbyPoi`，先检查小区数据范围。 |
| POI 反查管理小区 | `/search/poi-candidates`、`/search/reverse-communities` | POI 坐标与有权限的小区坐标计算直线距离。 |
| 空间边界 | `SpatialBoundaryController/Service` | 可保存/读取真实 GeoJSON；当前表中尚无边界数据。 |
| 本系统空间搜索 | `SpatialBoundaryService.search` | 按数据范围搜索小区/楼栋，不依赖外部地图服务。 |

### 不能夸大的部分

- 本轮没有重新联网调用高德，不能把本报告视为新的高德 Key 或网络连通性验收。
- `spatial_boundary=0`、`facility_placement=0`，因此不应把“已画好的多边形边界”或“已布置的设施点”作为当前数据成果展示。
- 坐标重复时源码明确记录告警而不做人为偏移，这一点可作为数据真实性设计说明。

---

## 13. MinIO & Floorplan Final State / MinIO 与户型图 V10 最终状态

### 数据库最终核验

| 项目 | 当前数量 |
|---|---:|
| `file_object` 元数据 | 12 |
| `layout_template_floor_plan` 默认模板映射（V9） | 4 |
| `layout_template_floor_plan_variant` 面积精确变体（V10） | 12 |
| `property_floor_plan` 单房覆盖映射 | 0 |

### 读取优先级

`FloorPlanService.download` 的源码路径严格为：

```text
1. property_floor_plan              单套房产上传覆盖
2. layout_template_floor_plan_variant  户型 + gross_area 精确变体
3. layout_template_floor_plan         户型默认图
4. 均不存在 → FLOOR_PLAN_NOT_FOUND
```

`PropertyUnitResponse` 含 `floorPlanSource`，可向前端解释当前预览来自房产、面积变体还是模板默认。V10 使用 `uk_layout_template_floor_plan_variant_area` 避免同一户型/同一建筑面积重复映射。

### 适合 PPT 的表述

“户型图不以 BLOB 写入 MySQL。对象二进制由 MinIO 保存，MySQL 保存对象元数据与业务关联；并且实现了房产特例图、同户型面积精确图、模板默认图三级回退。”

不要说“每套房都已上传专属户型图”，因为当前单房覆盖为 0；当前已落地的是模板/面积变体映射。

---

## 14. Transaction Evidence / 事务边界证据

源码中可直接看到 `@Transactional`，且多写入业务不依赖数据库 Trigger 或存储过程：

| 事务服务 | 关键一致性动作 |
|---|---|
| `ResidenceManagementServiceImpl` | 创建人员/入住、变更居住关系、更新房产状态和关联资格，或办理搬出。 |
| `ParkingPermitServiceImpl.review` | 锁定人员/房产，校验资格与名额，更新申请状态并在批准时写入车证。 |
| `ParkingPermitServiceImpl.submitMyApplication` | 校验居民身份、车辆归属、现有资格、有效期和重复待审申请后写入申请。 |
| `NotificationServiceImpl.send` | 写入通知和所有接收人；预览行为通过 after-commit 触发。 |
| `PropertyUnitService` | 房产创建/更新及资产审计协调。 |
| `FloorPlanService`、模板/变体导入服务 | 文件元数据与业务关联的事务处理，外部对象写入失败会转化为业务异常。 |
| `MeterSimulationServiceImpl` | 抄表模拟相关多写入操作。 |

答辩中可说“关键多表写入使用 Spring 声明式事务”，但不应说“由数据库存储过程或触发器自动维护”，因为当前数据库没有这两类对象。

---

## 15. Performance & Query Design / 性能与查询设计

本轮是静态审计，未运行 `EXPLAIN` 或压测；以下是可由库对象和源码直接确认的设计证据：

1. **真实分页**：房产、账单、缴费、居民、租赁、通知、车证等使用 `LIMIT #{limit} OFFSET #{offset}`，并配套 `count` 查询和前端统一 `PaginationBar`。
2. **账单组合视图**：`v_bill_financial_summary` 预先定义账单应收、实收、欠费的统一口径，统计/账单页面复用，降低多处重复聚合 SQL 的不一致风险。
3. **收费周期索引**：`idx_bill_period_status(billing_year, billing_month, status, bill_id)` 支持按年/月/状态的统计与筛选；`uk_bill_property_period(property_unit_id, billing_year, billing_month)` 防止同房同周期重复出账。
4. **高频关联索引**：例如 `idx_meter_reading_recorder`、`uk_meter_reading_time(meter_id, reading_at)`、`idx_payment_bill`、`idx_notification_recipient_person_read`、车证申请的人员/房产/状态/时间索引。
5. **范围先行**：Mapper 在 `WHERE` 中插入可访问小区条件，避免前端拿到全量后再过滤。
6. **排序白名单**：例如资产 Service 对排序字段使用允许集合，而不是拼接任意客户端字段。

可以在 PPT 中表述为“面向分页、周期统计、账单查询和数据范围过滤进行了索引与 SQL 设计”；不要在没有测量数据时声称具体 QPS、毫秒级性能或“完全消除 N+1”。

---

## 16. Course Requirement Mapping / 课程要求映射

### A. 课程明确重点（应在答辩中直接展示）

| 课程关注点 | 当前实现证据 | 答辩展示重点 |
|---|---|---|
| 需求分析与系统功能 | 多小区资产、人员入住、收费、通知、租赁、停车、空间服务 | 用一页业务模块总览说明“资产主线 + 多角色服务”。 |
| ER / 逻辑数据库设计 | 47 基础表、77 外键、50 唯一、108 CHECK；本报告第 4–5 节 | 主 ER 图只放 10–16 核心实体，解释产权/居住分离。 |
| 完整业务流程 | 入住、车证审核、通知、账单/缴费、户型图读取 | 选 2–3 条做现场全流程，其余放流程图。 |
| 数据规模 | 528 房产、606 人、18,273 账单明细、15,840 抄表 | 明确对照“实体 ≥30、关系 ≥200”。 |
| 统计功能 | 年度/月度、下钻、费用构成、入住率、Dashboard | 至少演示年度趋势、月度小区下钻、Dashboard 风险指标。 |
| 完整性与鲁棒性 | FK/UQ/CHECK、事务、统一异常 JSON | 展示一两个重复数据/非法状态被拒绝的案例或截图。 |

### B. 自然体现的工程能力（可以简洁说明）

- Spring Boot、Vue、MyBatis、MySQL 的前后端分层；复杂查询集中在 Mapper XML。
- Flyway V1–V10 的可追溯数据库演进。
- 统一响应、全局异常处理、Trace ID、业务访问日志。
- 后端分页、视图、索引、范围过滤和事务边界。

### C. 项目额外亮点（作为加分项，不喧宾夺主）

- 多小区数据范围控制，而不是只有单一管理员。
- JWT、刷新会话、居民门户与后台角色分流。
- MinIO 户型图对象存储与 V10 面积变体回退。
- 高德 JS 地图、数据库坐标、POI 周边与反查、空间边界预留。
- 通知的层级接收人展开、TO/CC 和收费载荷。
- 停车车证的居民申请、管理员审核、限额/价格规则和事务处理。

---

## 17. Recommended Screenshot Evidence / 建议最终截屏证据

本轮不采集截图。后续 PPT 建议由人工运行系统后截取以下真实界面：

1. 登录页与不同角色的入口分流。
2. 资产工作台：小区 → 楼栋 → 单元 → 房产层级及 4/14/40/528 规模。
3. 房产列表的真实分页、筛选和详情抽屉；抽屉内展示户型图来源。
4. 入住办理页面：选择人员、可入住房产、关系与日期校验。
5. 账单详情：账单、账单明细、成功缴费与欠费金额。
6. 年度收费趋势：完整 12 月横轴，1–7 月有数据，后续月无数据。
7. 月度小区下钻：从总览进入小区明细。
8. 通知目标层级选择与居民端通知列表。
9. 车证申请审核前后状态。
10. 高德底图、小区 Marker、放大后的楼栋 Marker、InfoWindow；如没有边界数据则不要截“已校准边界”。

---

## 18. Recommended Diagrams / 建议生成的图示

1. **系统架构图**：Vue → Axios → Spring Boot Controller/Service/Mapper → MySQL；MinIO、Redis readiness、AMap 独立画为外部基础设施。
2. **核心 ER 图**：采用本报告第 4 节的 15 实体版本，不使用 47 表全量图作为主图。
3. **简化关系模式图**：采用第 5 节，突出 PK/FK/UQ。
4. **收费数据流图**：抄表 → 账单明细 → 账单 → 成功缴费 → 财务视图 → Dashboard/统计。
5. **权限与数据范围图**：JWT → ActorContext → AuthorizationService → DataScopeService → Mapper SQL `IN (communityIds)`。
6. **户型图三级回退图**：单房覆盖 → 面积变体 → 模板默认 → MinIO。
7. **多角色业务流图**：管理员办理入住/审核车证/发通知；居民查看自身房产、账单、通知、车证。

---

## 19. Deferred / Do-Not-Claim List / 延后项与不能宣称项

| 项目 | 当前结论 | 后续建议 |
|---|---|---|
| Trigger | 未实现 | 若课程明确要求可单独设计审计型触发器，但不能假装当前已有。 |
| 存储过程 | 未实现 | 如需展示，可在独立需求确认后增加，不应为答辩临时伪造。 |
| Redis 业务缓存 | 未落地 | 当前仅 readiness/BYPASS；后续可针对读多写少指标设计明确缓存失效策略。 |
| 正式租赁合同 | 表结构存在但数据为 0 | 后续补合同和参与方的完整生命周期与样例数据。 |
| 停车位/停车权益 | 结构存在但数据为 0 | 后续补停车空间、权益配置与占用关系后再作为主业务演示。 |
| 中介账号角色绑定 | `account_agency_role=0` | 后续补真实中介账号与数据范围。 |
| 空间边界/设施布点 | 当前数据为 0 | 由人工校准真实 GeoJSON/设施位置后再展示。 |
| 单房专属户型图 | `property_floor_plan=0` | 当前可演示模板/面积变体；后续再补上传覆盖样例。 |
| 外部地图服务实时可用性 | 本轮未再联网验证 | 最终答辩前单独做一次人工网络验证即可。 |
| 性能基准 | 本轮未压测 | 如需量化结论，应另行做固定数据集的 EXPLAIN/压测。 |

---

## 20. Final Presentation Positioning / 最终答辩定位建议

建议把数据库答辩的中心放在以下一句话：

> **系统以“多小区—楼栋—单元—房产”空间资产层级为核心，通过独立的产权与居住关系管理真实人员状态；再将表具、账单、缴费、通知、车证、租赁和户型图关联到同一 MySQL 数据库，并以外键、唯一约束、CHECK、事务、角色权限和小区数据范围保障业务完整性。**

现场演示顺序建议：

```text
登录管理员
→ 资产工作台查看层级
→ 办理入住或查看已有入住
→ 查看账单与缴费
→ 年度统计并进行月度小区下钻
→ 发送通知或审核一个车证申请
→ 展示房产户型图与空间地图
```

这条路径既覆盖课程要求中的系统功能、完整业务流程、数据库设计、关系数据规模和统计能力，也避开了当前数据为 0 的合同、边界和停车位模块。

---

## 21. Audit Stop Boundary / 审计停止边界

本报告完成后停止。本轮没有：

- 修改 Java、Vue、Mapper XML、配置、启动脚本或迁移文件；
- 新增/修改/清空数据库 Schema 或任何业务数据；
- 启动、停止或占用后端、前端、Redis、MinIO 端口；
- 调用外部高德服务；
- 创建、修改或导出 PPTX。

