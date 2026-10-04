# 数据库性能、事务一致性与并发风险最终审计

> 审计日期：2026-08-31  
> 范围：`_MERGED_V1` 当前源码、MySQL `community_db` 的只读查询、`EXPLAIN` / 一次只读 `EXPLAIN ANALYZE`。  
> 边界：本轮未修改 Java、Vue、Mapper、Migration、索引或数据库数据；未执行并发写入、压测或破坏性实验。

## 1. Executive Summary

- 当前数据库运行于 **MySQL 8.0.43**，真实隔离级别为 **REPEATABLE-READ**，会话 `autocommit=1`；Spring 的 `@Transactional` 未显式指定 isolation，因此事务采用数据库默认隔离级别。
- 当前规模已不是“几个演示数据”：4 个小区、14 栋楼、528 套房、1,584 块表计、15,840 条表计读数、3,696 张账单、18,273 条账单明细、3,324 笔缴费。
- 大多数日常实体维护是“实体表 + 审计表”的短事务；入住/退租、停车证审核、通知发送、表计模拟是当前真正的多表事务。
- 当前最明确的锁定读有两处：刷新令牌的 `SELECT ... FOR UPDATE`，以及车证审核中按主键锁 `person`、`property_unit`。两者都采用精确键条件，推断为行级记录锁，而非全表锁。
- 当前未发现全表 `UPDATE`、范围 `UPDATE`、`DELETE` 或 `LOCK IN SHARE MODE`。不存在已证实的死锁；可识别的并发热点是停车证审核、同一房产的入住/退租，以及每个计量时刻的表计读数批量写入。
- **重要限制**：当前源码中没有线上“生成 Bill / BillItem”或“写入 Payment / 回写 Bill”的 Service/Mapper 写入口；它们的现有数据来自演示/迁移数据并供列表、统计和 View 使用。因此不能宣称系统已验证“批量账单生成与缴费同时写入”的事务行为。
- 最值得课堂展示的索引不是“索引越多越好”，而是：`meter_reading(meter_id, reading_at)`、`bill(property_unit_id, billing_year, billing_month)`、`bill(billing_year, billing_month, status, bill_id)`。三者分别服务时间序列读取、账期唯一性与统计筛选。

## 2. 当前数据库规模

以下为审计时的 `COUNT(*)` 精确值；`information_schema.tables.table_rows` 为 InnoDB 估值，未替代这些计数。

| 表 | 精确行数 | 与性能的关系 |
|---|---:|---|
| `community` | 4 | 多小区数据作用域起点 |
| `building` | 14 | 空间层级 Join |
| `property_unit` | 528 | 房产、账单、入住的核心维度 |
| `person` | 606 | 人员、缴费人与入住关联 |
| `residence` | 597 | 当前入住和历史入住事实 |
| `meter` | 1,584 | 约每房三块表计 |
| `meter_reading` | 15,840 | 连续增长的时间序列事实 |
| `bill` | 3,696 | 房产×账期账单事实 |
| `bill_item` | 18,273 | 账单明细聚合来源 |
| `payment` | 3,324 | 实收金额聚合来源 |

数据规模说明：在 1 个小区、少量房产时，列表与统计即使缺少合适索引也不易暴露问题；而目前的表计、账单、明细和缴费已经形成持续增长的事实表。尤其 View 聚合会读取 `bill_item` 与 `payment`，其成本不会随着前端分页而自动消失。

## 3. Transaction Inventory

所有扫描到的 `@Transactional` 均未显式设置 `readOnly`、`propagation` 或 `isolation`：默认分别为 `false`、`REQUIRED`、`DEFAULT`。`DEFAULT` 在本库中落到 `REPEATABLE-READ`。

| Service / Method | 业务 | 事务属性 | 主要写表 | 主要读表 |
|---|---|---|---|---|
| `AuthService.login` | 登录、失败次数与刷新令牌签发 | 默认 | `app_account`、`refresh_token_session` | `app_account`、角色表 |
| `AuthService.refresh` | 刷新令牌轮换 | 默认 | `refresh_token_session` | `refresh_token_session`、`app_account` |
| `AuthService.logout` | 撤销当前刷新令牌 | 默认 | `refresh_token_session` | — |
| `AccountBootstrapRunner.run` | 本地启动账号对账 | 默认 | `app_account`、`account_global_role` | 账号、角色 |
| `DemoAccountProvisionServiceImpl.provisionStageGDemoAccounts` | 演示物业人员/居民账号批量准备 | 默认 | `app_account`、`account_global_role`、`account_community_scope` | 小区、候选人员、角色 |
| `CommunityService.create/update` | 小区维护 | 默认 | `community`、`asset_change_audit` | `community` |
| `BuildingService.create/update` | 楼栋维护 | 默认 | `building`、`asset_change_audit` | `building`、`community` |
| `BuildingUnitService.create/update` | 单元维护 | 默认 | `building_unit`、`asset_change_audit` | `building_unit`、`building` |
| `PropertyUnitService.create/update` | 房产维护 | 默认 | `property_unit`、`asset_change_audit` | `property_unit`、`building_unit` |
| `ResidenceManagementServiceImpl.moveIn` | 办理入住 | 默认 | `property_unit`、`residence`，租赁时还写 `residence_parking_eligibility` | `person`、`ownership`、`property_unit` |
| `ResidenceManagementServiceImpl.moveOut` | 办理退租 | 默认 | `residence`、`property_unit` | `residence`、`rental_listing` |
| `MeterSimulationServiceImpl.simulateNow` | 手动模拟表计读数 | 默认 | `meter_reading`（批量） | `meter`、`fee_type`、`meter_reading` |
| `MeterSimulationServiceImpl.simulate` | 内部模拟实现 | 标注默认；见下方代理限制 | `meter_reading`（批量） | 同上 |
| `NotificationServiceImpl.send` | 发通知及建立收件人关系 | 默认 | `notification`、`notification_recipient` | 范围内人员/空间层级 |
| `ParkingPermitServiceImpl.review` | 审核申请并签发/拒绝车证 | 默认 | `parking_permit_application`、批准时 `parking_permit` | `person`、`property_unit`、资格、申请、既有车证 |
| `ParkingPermitServiceImpl.createMyVehicle` | 居民登记车辆 | 默认 | `vehicle` | `vehicle` |
| `ParkingPermitServiceImpl.submitMyApplication` | 居民提交车证申请 | 默认 | `parking_permit_application` | 车辆、产权/入住资格、既有申请/车证 |
| `ResidentPortalServiceImpl.submitIncident` | 居民提交报事 | 默认 | `resident_incident` | `residence` |
| `FloorPlanService.upload` | 上传房产户型图 | 默认 | `file_object`、`property_floor_plan`，旧文件软删除 | 房产、旧户型图 |
| `TemplateFloorPlanImportService.importStandardPlans` | 导入模板标准户型图 | 默认 | `file_object`、`layout_template_floor_plan` | 户型模板、已有文件 |
| `FloorPlanVariantImportServiceImpl.importStandardVariants` | 导入面积变体户型图 | 默认 | `file_object`、`layout_template_floor_plan_variant` | 户型模板、变体、已有文件 |

未发现 `TransactionTemplate`。`FloorPlanService.upload` 使用 `TransactionSynchronization.afterCommit()`：旧对象仅在数据库提交成功后才异步式地发起 MinIO 删除，避免“数据库已回滚却先删掉旧文件”。

### 3.1 事务边界与大事务判断

| 事务 | 实际工作量 / 外部调用 | 风险 | 结论 |
|---|---|---|---|
| 资产维护、登录、登出、车辆、报事 | 1–2 个主要写表；无外部调用 | LOW | 短事务，精确键写入为主 |
| 入住 / 退租 | 2–3 个表；房产版本条件更新 | MEDIUM | 多表原子性合理，未见网络调用；冲突时以受影响行数为 0 拒绝 |
| 令牌刷新 | 锁定一个刷新令牌后更新、插入替换会话 | LOW | 锁对象由唯一 `token_hash` 精确定位，链路短 |
| 车证审核 | 两条 `FOR UPDATE`、申请状态更新、可能插入车证 | MEDIUM | 正确地先锁人员再锁房产；应保持此顺序 |
| 表计手动模拟 | 当前最多可处理 1,584 块 ACTIVE 表计，组装一次多值 `INSERT IGNORE` | MEDIUM | 不是网络长事务，但批量 SQL 和唯一检查会集中写入较多索引页 |
| 通知发送 | 1 条通知 + 按范围批量插入多个收件关系 | MEDIUM | 当前已有 507 条收件关系；目标范围扩大时，事务成本随收件人数增长 |
| 演示账号批量准备 | 最多 4 个工作人员和 50 个居民，循环中执行 BCrypt 与多次 Mapper | MEDIUM | 非高频线上路径，但 CPU 计算在事务内，宜作为运维/演示操作而非日常请求 |
| 房产图上传 | 读取最多 10MB、调用 MinIO `putObject` 后写两表 | HIGH | 外部对象存储与文件 I/O 位于事务内；虽未在前面取得锁，但会延长连接/事务生命周期 |
| 两类标准户型图导入 | 在事务内循环读取资源并多次 MinIO 上传、写入关联表 | HIGH | 批量导入 + 外部服务；其中 Variant 导入计划包含多份变体，最接近长事务 |

结论：**当前未发现“全表更新式”的明显大事务；但三类 MinIO 导入/上传是明确的外部 I/O 事务，表计批量模拟是明确的批量写事务。**

### 3.2 Meter 模拟的代理边界注意事项

`simulateNow()` 是公开且带 `@Transactional` 的 Spring 入口，调用内部 `simulate(...)` 时会加入同一个事务。反之，`scheduledTick()` 在同一实例内直接调用 `simulate(...)`；按 Spring 代理的自调用语义，后者的注解不会重新开启事务。该调度路径中的“查已有读数 + 批量插入”可能以自动提交方式分段执行。

这不是数据写入失败的证据：`INSERT IGNORE` 与 `uk_meter_reading_time(meter_id, reading_at)` 会抑制同一表计同一时刻的重复行。但它意味着调度路径不应被描述为“完整的显式跨语句事务”。

## 4. Locking Model

### 4.1 代码直接可见的锁定事实

| 业务 | SQL 类型 | 条件 | 是否命中 PK / UK | 代码事实 |
|---|---|---|---|---|
| 刷新令牌 | `SELECT ... FOR UPDATE` | `token_hash = ?` | 是，`uk_refresh_token_hash` | `AuthMapper.findRefreshSessionForUpdate` 锁定当前令牌会话 |
| 车证审核 | `SELECT ... FOR UPDATE` | `person_id = ?` | 是，`person.PRIMARY` | `ParkingPermitMapper.lockPerson` |
| 车证审核 | `SELECT ... FOR UPDATE` | `property_unit_id = ?` | 是，`property_unit.PRIMARY` | `ParkingPermitMapper.lockProperty` |
| 入住抢占 | `UPDATE` | `property_unit_id=? AND version=? AND status/occupancy...` | 是，主键精确更新 | `claimProperty` 通过版本号/状态条件乐观防冲突 |
| 退租释放 | `UPDATE` | `residence_id=? AND end_date IS NULL` | 是，主键精确更新 | `endResidence` |
| 退租释放 | `UPDATE` | `property_unit_id=? AND version=? ...` | 是，主键精确更新 | `releaseProperty` |
| 车证审核状态 | `UPDATE` | `parking_permit_application_id=? AND application_status='PENDING'` | 是，主键精确更新 | `approveApplication` / `rejectApplication` |
| 表计读数 | `INSERT IGNORE` 批量 | `(meter_id, reading_at, ...)` | 是，唯一键检查 | `MeterSimulationMapper.insertReadings` |
| 通知投递 | 多值 `INSERT` | `notification_id, person_id, ...` | 是，唯一 `uk_notification_recipient_once` | `NotificationMapper.insertRecipients` |

未发现 `LOCK IN SHARE MODE`、`FOR SHARE`、全表 `UPDATE`、范围 `UPDATE` 或应用 Mapper 中的 `DELETE`。

### 4.2 根据 InnoDB 行为的锁推断（非运行时锁监控结论）

- 上表中按主键或唯一键**精确命中**的 `UPDATE` / `FOR UPDATE`，在 InnoDB 的 `REPEATABLE-READ` 下通常持有目标记录的排他锁或锁定读记录锁，直至事务提交；不是“锁全表”。
- `claimProperty` / `releaseProperty` 的条件中虽有状态和版本，但访问路径仍由 `property_unit_id` 主键定位，因此锁范围通常是一条房产记录；版本不符时更新 0 行并在业务层返回冲突。
- `INSERT` 会对新增记录、唯一索引检查和外键检查施加必要的内部锁。`meter_reading` 的唯一键与 `notification_recipient` 的唯一键同时提供正确性保护，也带来同键并发时的等待或重复键处理成本。
- 当前没有从 Mapper 直接看到“非唯一索引范围更新”，所以不能把 next-key/gap lock 说成当前系统已经发生的事实。若未来增加 `UPDATE ... WHERE status=...` 之类范围写，在 `REPEATABLE-READ` 下才需要专门复测 next-key / gap lock。

## 5. Isolation / Consistent Reads

- 审计查询结果：`@@transaction_isolation = REPEATABLE-READ`，`@@autocommit = 1`。
- Spring `@Transactional` 没有覆盖 isolation；普通非事务读请求不显式开启读事务，按连接的自动提交行为完成。
- Mapper 中的房产、人员、账单、缴费、统计、Dashboard、数据作用域查询均为普通 `SELECT`，没有锁定子句；在 InnoDB 下通常属于一致性读 / 快照读，不会像写事务一样长期持有行锁。
- `AuthMapper.findRefreshSessionForUpdate`、`ParkingPermitMapper.lockPerson`、`ParkingPermitMapper.lockProperty` 是当前明确的锁定读，必须在事务内才有“读到提交前持续持锁”的意义。
- 普通 View 统计也不是锁定读。它会消耗 CPU、扫描和临时聚合资源，但不等价于对账单/缴费行施加写锁。

## 6. MeterReading – Bill – Payment 并发矩阵

### 6.1 真实写入链路

**MeterReading**：`MeterSimulationMapper` 先查 ACTIVE 表计和该时刻已存在的 `meter_id`，随后用一次多值 `INSERT IGNORE INTO meter_reading(...)` 写入。它不更新 `meter`，不写 `bill`，也不写 `bill_item`。

**Bill / BillItem**：当前代码仅存在列表、详情、统计 Mapper；全项目源码未发现 `INSERT INTO bill`、`INSERT INTO bill_item` 或线上账单生成 Service。因此本轮无法列出“账单生成事务”，也不能凭数据库已有账单反推它曾经如何加锁。

**Payment**：当前代码仅存在 `payment` 查询、View 聚合和迁移/演示数据；全项目源码未发现 `INSERT INTO payment` 或“支付后更新 bill”的业务代码。当前实收金额由 View 对 `payment.status='SUCCESS'` 的 `SUM(payment.amount)` 得到，不是通过 `bill` 行上的已收金额字段回写。

| 操作 A | 操作 B | 是否写同表 | 是否可能写同一行 | 争用风险 | 依据 |
|---|---|---|---|---|---|
| MeterReading 写入 | Payment 写入 | 否（当前 Payment 无写入口） | 否 | LOW | 表计模拟只写 `meter_reading`；Payment 当前只读可审计 |
| Bill Generation | Payment | 当前无可审计实现 | 无法确认 | UNKNOWN | 两类线上写入口未在当前源码中实现 |
| MeterReading 写入 | Bill Generation | 当前无可审计实现 | 无法确认 | UNKNOWN | 同上；不要从表结构推断完整事务 |
| Payment 写入 | 财务统计 SELECT | 当前 Payment 无写入口 | 统计不写行 | LOW（推断） | 统计为普通一致性读；未做并发实测 |
| MeterReading 模拟 | 表计历史 SELECT | `meter_reading` 一写一读 | 读不写同一行 | LOW–MEDIUM | 读写会竞争 I/O/Buffer Pool，不等价于长期行锁争用 |
| 车证审核 | 车证审核（同人或同房产） | 是 | 是 | MEDIUM | 显式按 `person`、`property_unit` 顺序 `FOR UPDATE` |
| 入住 / 退租 | 同一房产入住 / 退租 | 是 | 是 | MEDIUM | 均触及 `property_unit`；版本/状态条件会让后到请求冲突或等待 |

## 7. 高频查询、分页与 N+1

| 页面 / Mapper | 读取类型 | 分页与 N+1 审计 |
|---|---|---|
| 房产 `PropertyMapper.selectPropertyPage` | A. 普通一致性读 | 数据库 `LIMIT/OFFSET` + 独立 `COUNT(*)`；不是 Java 全量分页 |
| 人员 `ResidentMapper` | A. 普通一致性读 | 页面查询与 `COUNT(*)` 分开执行；源码未见列表行循环查询 Mapper |
| 账单 `BillMapper.selectBillPage` | A. 普通一致性读 | `LIMIT/OFFSET` + `COUNT(*)`；详情固定执行 base、items、payments 三个查询，不是按列表行数增长的 N+1 |
| 缴费 `FeeCenterMapper.selectPaymentPage` | A. 普通一致性读 | `LIMIT/OFFSET` + `COUNT(*)`，多表 Join 在单 SQL 内完成 |
| 停车 `ParkingPermitMapper.selectApplicationPage/selectPermitPage` | A. 普通一致性读 | 数据库分页；没有按每条申请再循环查询的证据 |
| 通知 `NotificationServiceImpl.pageSent/pageInbox` | A. 普通一致性读 | 数据库偏移分页，页大小限制为 100 |
| 月度/年度统计、Dashboard | A. 普通一致性读 | 聚合 SQL / View；不是锁定读，但会扫描账单、明细、缴费聚合来源 |
| 数据作用域查询 | A. 普通一致性读 | 通过 `community_id IN (...)` 嵌入同一 SQL，而非逐小区循环查询 |

当前所有主要列表都使用 `LIMIT ... OFFSET ...`。当页码非常深时，数据库仍需跳过前面的行，存在 OFFSET 深分页的潜在成本；当前规模下未做性能阈值测试，故仅记录为后续增长风险，不建议本轮改造。

可见的循环 Mapper 调用主要位于受控导入/演示流程：Stage G 演示账号准备（至多 50 位居民候选）以及标准户型图变体导入。它们不是高频表格页面的 N+1，但属于批量事务成本来源。

## 8. Index Inventory

### 8.1 关键索引及其实际作用

| 业务域 | 主键 / 唯一约束 | 普通 / 联合索引 | 审计结论 |
|---|---|---|---|
| 空间层级 | `community_code`、`(community_id, building_no)`、`(building_id, unit_no)`、`(building_unit_id, property_no)` | `property_unit.layout_template_id` | Join 多经由各表主键；层级业务键保证编号不重复 |
| 人员与入住 | 人员编码/证件号；`residence` 当前人员、当前责任房产及业务键唯一 | `residence(property_unit_id)` | 房产维度查当前入住有支持；多重唯一键用于“当前关系”约束 |
| 表计 | `meter_no`；`meter_reading(meter_id, reading_at)` 唯一 | `meter(property_unit_id)`、`meter(fee_type_id)`、`meter_reading(recorder_person_id)` | 时间范围读取正确命中 `(meter_id, reading_at)` 左前缀 |
| 账单 | `bill_no`、`(property_unit_id,billing_year,billing_month)` | `idx_bill_period_status(billing_year,billing_month,status,bill_id)` | 唯一键支持“一套房一账期一账单”；联合索引服务账期/状态报表 |
| 明细与缴费 | `bill_item` 主键 `(bill_id,item_no)`；缴费单号/外部流水唯一 | `bill_item` 的 fee/读数外键索引；`payment(bill_id)`、`payment(payer_person_id)` | 账单详情和 Payment 按 bill 查询有直接索引支撑 |
| 通知 | `notification_recipient(notification_id,person_id)` 唯一 | 收件人未读、发件时间、人员已读状态联合索引 | 防重复投递，并服务收件箱/未读查询 |
| 停车 | 申请号、车证号、申请→车证唯一 | 申请按房产/状态/创建时间；车证按房产或持有人/有效期；车辆外键索引 | 审核和列表的常用维度有索引 |

### 8.2 索引质量判断

1. **JOIN 字段**：`bill.property_unit_id`、`payment.bill_id`、`meter.property_unit_id`、`residence.property_unit_id` 等关键外键都有主键、唯一键或支持索引；当前 Join 不需要全表嵌套扫描作为默认路径。
2. **WHERE 条件**：`meter_id + reading_at`、房产+账期、账期+状态均有实际索引。审计 `EXPLAIN` 已验证前两类。
3. **ORDER BY / GROUP BY**：账期统计仍可能在 View 展开后进行派生表聚合和排序；索引能缩小 `bill` 起始集合，但不能替代对 `bill_item`、`payment` 的按账单聚合。
4. **冗余索引**：未发现 `uk_bill_property_period` 与 `idx_bill_period_status` 的简单重复；它们的左前缀分别是 `property_unit_id` 与 `billing_year`，服务不同访问路径。
5. **低选择性 `status`**：未看到脱离业务前缀的通用 `bill(status)` 索引。`status` 位于账期联合索引后，避免了单独为低选择性状态值建立大索引。`residence_parking_eligibility(eligible)` 较低选择性且表很小，是将来真实量增长后才值得复测的候选，不是本轮新增索引理由。
6. **写入代价**：`meter_reading` 每插入一行需维护主键、唯一时间键和外键相关结构；`bill_item` 的多个读数/费率关联索引也会增加写入成本。这是正确性与读取性能的现实交换，不应盲目追加索引。

## 9. Candidate Index Experiments

以下均来自当前 Mapper 的真实查询形状。可在未来使用测试账号、只读 `EXPLAIN` / `EXPLAIN ANALYZE`，并以 `IGNORE INDEX` 做同一 SQL 的非破坏性对照；**不需要也不应 DROP INDEX**。

| 候选 | 来源与真实 SQL 形状 | 当前计划证据 | 可做的非破坏性对照 | 预期观察 |
|---|---|---|---|---|
| A. 表计时间范围 | `MeterSimulationMapper.selectExistingMeterIds` / 同域历史查询：`WHERE meter_id=? AND reading_at>=? AND reading_at<?` | 实测 `EXPLAIN`：`type=range`、`key=uk_meter_reading_time`；`EXPLAIN ANALYZE` 为 Index range scan，实际 1 行、约 0.012–0.015ms | 在同一 `SELECT` 的 `meter_reading IGNORE INDEX (uk_meter_reading_time)` 上比较 | `type`、扫描行数、实际耗时由 range/index condition 向更大扫描变化 |
| B. 账单按账期筛选 | `BillMapper.selectBillPage`：View 上按 `billing_year`、`billing_month`、可选 `status` 过滤 | 账期统计形状实测 `bill` 使用 `idx_bill_period_status`，`type=ref` | 在展开后的 `bill b` 节点加 `IGNORE INDEX (idx_bill_period_status)`，保留同一过滤/Join | 观察是否失去账期起始筛选，以及派生聚合前的候选行变化 |
| C. 年度费用构成 | `StatisticsMapper.selectFeeComposition`：`bill JOIN bill_item ... WHERE b.billing_year=? GROUP BY fee_type` | 同年度统计计划中，`bill` 走 `idx_bill_period_status`；`bill_item` 按 `(bill_id,item_no)` 聚合/访问 | 在 `bill b` 使用 `IGNORE INDEX (idx_bill_period_status)` 做 `EXPLAIN` 对照 | 观察年度账单集合的 access type、rows examined，以及聚合代价 |

实测说明：年度 View 查询的执行计划显示 `payment` 与 `bill_item` 派生聚合会读取其索引页（`payment.idx_payment_bill`、`bill_item.PRIMARY`）。这正适合课堂说明：索引先缩小主表条件，不表示聚合明细表“零成本”。

## 10. Views and Aggregation

### `v_bill_financial_summary`

- 基础表：`bill`、按 `bill_id` 聚合的 `bill_item`、按 `bill_id` 聚合且 `status='SUCCESS'` 的 `payment`。
- 核心字段：应收 `SUM(bill_item.amount)`、实收 `SUM(payment.amount)`、欠费 `GREATEST(应收-实收,0)`。
- 作用：封装账单财务口径，使列表、欠费、Dashboard、统计复用同一逻辑。

### `v_monthly_collection_summary`

- 基础对象：`v_bill_financial_summary`。
- `GROUP BY`：`billing_year, billing_month`。
- 聚合：账单数、应收、实收、欠费、收缴率。

两者均是普通 MySQL View：**主要封装查询逻辑，并不等同于物化视图或预计算缓存。**查询时优化器仍会展开并处理底层 `bill_item`、`payment` 聚合；不能把 View 本身描述为“预先算好、读取零成本”。

## 11. FK / UNIQUE / CHECK 与写入成本

| 业务事实 | 真实约束 | 一致性收益 | 并发写入代价 / 失败表现 |
|---|---|---|---|
| 当前 Residence | `uk_residence_current_person`、`uk_residence_current_responsible_property` 与业务日期约束 | 防止一个人或责任房产出现不符合当前状态的重复关系 | 并发建立同类关系可能得到重复键；入住还通过房产版本更新产生业务冲突 |
| 一房一账期 Bill | `uk_bill_property_period(property_unit_id,billing_year,billing_month)` | 防止同一房产同一账期重复账单 | 并发生成时会发生唯一检查/可能等待或 duplicate key |
| 表计同刻读数 | `uk_meter_reading_time(meter_id,reading_at)` | 防重复采集；当前 `INSERT IGNORE` 可幂等落库 | 唯一索引页写入和冲突检查；重复行被忽略而不是新增 |
| Notification 收件人 | `uk_notification_recipient_once(notification_id,person_id)` | 同一通知不会重复投递给同一人 | 批量投递时进行唯一检查 |
| Permit Application → Permit | `uk_parking_permit_application` | 一份申请最多签发一个车证 | 并发审核时通过申请状态条件更新与唯一键共同保护 |
| 外键与 CHECK | Bill、Payment、MeterReading、Permit 等多表均有 FK/CHECK | 拦截不存在父记录、金额/状态/日期非法值 | 插入时进行 FK/检查约束验证；出现 FK 失败或锁等待是正确性保护的成本 |

## 12. Deadlock Risk

本轮未读取到任何死锁日志，也未运行并发写实验；因此**没有证据证明发生过死锁，也不能宣称零死锁。**

- 已有积极事实：车证审核固定先 `lockPerson`、后 `lockProperty`，当前代码路径一致，降低同一业务内 AB/BA 锁顺序的风险。
- 入住流程先条件更新 `property_unit` 再插入 `residence`；退租流程先更新 `residence`，在最后一个有效入住结束时再更新 `property_unit`。二者写表顺序不同，但正常业务状态（空房入住与在住退租）通常不针对同一有效关系；现阶段只可标为**潜在且未实证**的同房产竞争点。
- 车证申请提交在“检查是否已有 PENDING”后直接插入，数据库没有 `(holder_person_id, vehicle_id, property_unit_id, application_status)` 的唯一约束。两个并发提交可能都通过前置检查并产生两条 PENDING 申请；这是业务重复风险，尚不是死锁证据。

## 13. Datasource / Connection Pool

来自 `backend/src/main/resources/application.yml` 与数据库会话的事实：

| 项目 | 当前值 |
|---|---|
| Hikari maximum pool size | 10 |
| Hikari minimum idle | 2 |
| Hikari connection timeout | 10,000ms |
| MyBatis default statement timeout | 10 秒 |
| Spring transaction timeout | 未显式设置（不在本轮猜测具体框架默认秒数） |
| Hikari `auto-commit` | 未显式设置 |
| 当前数据库会话 `@@autocommit` | 1 |
| JDBC batch 参数 | 未显式设置 |
| 实际批量写 | `MeterSimulationMapper.insertReadings` 使用单条多值 `INSERT IGNORE`，不是 JDBC rewrite batch 配置 |

## 14. Controlled Experiment Plan（本轮不执行）

目标：在不污染正式业务数据的前提下，观察“表计持续写入、缴费写入、财务统计读取”并存时的耗时、锁等待和错误。

1. 建立**专用测试小区 / 房产 / 表计 / 账期**，所有测试号带固定前缀；不复用当前课堂演示记录。
2. 线程 A：对专用表计按不同时间戳插入读数；先测试唯一键冲突，再测试正常时间序列写入。
3. 线程 B：仅在未来实现 Payment 写入口后，向专用账单写入支付；当前版本不伪造该接口。
4. 线程 C：持续运行 `StatisticsMapper.selectMonthlyCollection` / `selectFeeComposition` 的只读版本，记录 p50/p95 耗时。
5. 观察：应用 SQL 耗时、MySQL `performance_schema` 锁等待（若已启用）、死锁状态、成功/duplicate key 数、事务延迟。
6. 收尾：在隔离的测试 schema 或明确测试前缀下，使用预先批准的清理脚本回滚/删除测试数据；不触碰正式课堂数据。

## 15. PPT-ready Conclusions

可直接用于课程汇报的事实：

1. 当前系统的入住抢占、退租释放、车证审核和刷新令牌轮换使用短事务或精确行锁；它们不是全表加锁。
2. 表计时间序列通过 `(meter_id, reading_at)` 唯一联合索引避免同刻重复读数，并让“某表计 + 时间范围”走索引范围扫描。
3. 账单按房产和账期的唯一键保证“一套房、一个账期、一张账单”；年度账期查询由 `(billing_year, billing_month, status, bill_id)` 提供起始筛选支持。
4. Dashboard 与月度/年度统计是普通一致性读；它们会消耗聚合资源，但不是当前明确的行锁长期持有者。
5. 普通 View 将财务口径集中在数据库侧，减少多处重复 SQL；但 View 不是物化视图，仍需读取并聚合底表。
6. 真实规模下，最有教学价值的矛盾是：索引降低读成本，但每增加一个索引也会增加 `MeterReading`、`BillItem`、`Payment` 等事实写入的维护成本。
7. 当前最适合展示的实验是：`EXPLAIN ANALYZE` 对照表计范围索引，以及对账期账单/费用构成查询的 `IGNORE INDEX` 非破坏性对照。

## 16. DO NOT CLAIM

以下表述没有本轮证据，课堂汇报中不应使用：

- “系统已支持高并发。”（未执行压力或受控并发实验）
- “系统实现了零死锁。”（未做死锁实验，也未读取死锁历史）
- “Redis 大幅提高了数据库查询性能。”（本轮未证明 Redis 承担查询缓存）
- “View 是预计算缓存 / 物化视图。”（当前是普通 MySQL View）
- “账单生成与缴费写入已经完成并发一致性验证。”（当前源码不存在这两类线上写入实现）
- “所有 `@Transactional` 都对每一种调用路径生效。”（表计调度路径存在同类自调用代理边界）

