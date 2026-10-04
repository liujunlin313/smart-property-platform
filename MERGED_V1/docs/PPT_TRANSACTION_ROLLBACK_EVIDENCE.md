# PPT 事务回滚证据审计：入住办理与停车资格原因失败

> 审计性质：只读取证。没有重新执行失败请求、没有插入测试数据、没有修改业务代码、数据库、迁移或 PowerPoint。
>
> 取证时间：2026-08-31；目标历史事件：2026-08-30 16:36:06（Asia/Shanghai）。

## 1. 结论摘要

2026-08-30 的真实问题不是 JSON 语法错误，也不是“返回 200 后又被 204 覆盖”。当时管理员先单独成功新增了一名人员（`person_id=1028`），随后提交“租赁入住 + 暂不允许申请车证”。入住事务已经顺序执行了房产占用更新和入住记录插入，但在写入 `residence_parking_eligibility.reason_code` 时被 MySQL 的 CHECK 约束拒绝，业务请求最终返回 **HTTP 500**。

直接根因是：旧前端把“暂不允许”分支中的**自由文本原因**原样放进 `parkingIneligibleReasonCode`，旧服务层只检查该字段非空并原样写库；而数据库只接受四个稳定机器码。历史日志为安全起见没有记录绑定参数，因此无法可靠恢复该次用户输入的中文原文，也不能把“刘/刘刘”这一记忆当作事实。

`moveIn` 本身处于 Spring `@Transactional` 边界内，错误以未捕获的 `UncategorizedSQLException` 向外抛出，且堆栈经过 `TransactionInterceptor`。结合同一人员后续能够重新办理、当前数据库仅留下后一次成功入住这一事实，可以确认**入住事务内部的房产占用更新和入住插入未形成残留**。但“新增人员”是先前的独立 HTTP 200 请求，不属于这一个事务，因而不会回滚。

建议本次 PPT 取证定性为 **Outcome B（可讲的局部真实事务回滚案例）**：真实约束失败、真实写入顺序、真实事务边界、真实无半完成入住残留均有证据；但不应包装成“新增人员 + 入住全流程统一回滚”的案例。

---

## 2. 事件身份与时间线

| 时间 | 事件 | 结果 | 证据 |
|---|---|---|---|
| 16:35:55.598 | `POST /api/admin/residence-management/people` | 200，新增人员 | `access.log` 575–583（见第 13 节） |
| 16:35:55.617 | 查询该人员可办理房产 | 200，`personId=1028` | `access.log` 576–583 |
| 16:35:58.728 | 前端埋点：新增人员并选择 | 204，观测上报成功 | `frontend-client.log:22` / `access.log:584–586` |
| **16:36:06.246** | `POST /api/admin/residence-management/move-ins` | **500** | `access.log:587–603`，trace `503596760319` |
| 16:36:06.242 | MySQL 停车资格原因 CHECK 失败 | SQL 3819 | `backend.log:46–54` |
| 16:36:06.252 | 前端上报“HTTP 请求失败” | **204**，仅观测接口 | `frontend-client.log:23–24` / `access.log:604–606` |
| 16:38:54.292 | 同一人员再次办理入住 | 200，完整插入成功 | `access.log:607–625`，trace `FF837BC1C46F` |

关键区分：业务失败 trace 是 `503596760319`；前端事件 trace 是 `DB5D9FDA89D5`。两者不是同一 HTTP 请求。

---

## 3. 历史运行日志恢复

已恢复的原始运行目录：

`C:\Projects\数据库课程设计\多小区智慧物业管理系统_MERGED_V1\logs\run-20260830-163409-707-p34768`

主要文件与可截图行段：

| 文件 | 行段 | 可证明内容 |
|---|---:|---|
| `access.log` | 575–606 | 新增人员 200、失败入住 500、独立 client-event 204 |
| `access.log` | 607–625 | 后续重试 200 与全部六步 Mapper/SQL 链路 |
| `backend.log` | 46–54 | CHECK 名称、SQL、MySQL 3819、500 的异常根因 |
| `backend.log` | 77–78 | Spring `TransactionInterceptor` 调用栈证据 |
| `frontend-client.log` | 22–24 | 前端“新增人员并选择”以及“确认办理入住”后收到 500 |

项目目录没有 `.git` 元数据，因此不存在可供本次审计查看的 Git 提交历史。原始失败时的旧版 Vue/Java 源文件也未在项目中保留；关于旧页面是“自由文本原因”以及原样传递，采用了项目内已有的历史热修复报告 `docs/STAGE_UI_R2_HOTFIX2_PARKING_ELIGIBILITY_CONTRACT_REPORT.md` 与上述运行日志交叉验证。当前可访问范围内没有可直接检索、可引用的旧 Codex 原始操作记录或浏览器 Network 请求体；此限制已体现在第 6、12、16 节。

---

## 4. 前端请求字段与旧版契约问题

当前请求 DTO 的字段仍明确反映该业务契约：

```json
{
  "personId": 1028,
  "propertyUnitId": "<未在历史安全日志展开>",
  "startDate": "<日期>",
  "expectedEndDate": "<日期>",
  "moveInType": "TENANT",
  "parkingApplicationEligible": false,
  "parkingIneligibleReasonCode": "<旧版自由文本，不是允许机器码>",
  "parkingEligibilityRemark": "<可选自由文本>"
}
```

字段链：

`MoveInView.vue` → `residenceApi.moveIn(...)` → `POST /api/admin/residence-management/move-ins` → `MoveInRequest`。

现行页面已把这一错误收口：`frontend/src/views/residency/MoveInView.vue:10–23` 定义受控下拉，中文 label 与机器码 value 分离；false 分支仅发送选中的 `parkingIneligibleReasonCode`，而备注仍是 `parkingEligibilityRemark`。

旧版确实存在的字段层错误是 **reason code 与自由文本混用**；不是 `JSON.parse`、字段反序列化失败或 JSON 格式不合法。历史日志没有记录参数值，故不能写出那次具体字符串，更不能断言它是“刘”或“刘刘”。

---

## 5. API、Controller、Service、Mapper 调用链

失败请求的历史 Flow 日志（`access.log:587–603`）记录：

1. `ResidenceManagementController.moveIn`
2. `ResidenceManagementServiceImpl.moveIn`
3. `AuthorizationServiceImpl.requirePermission`
4. `DataScopeServiceImpl.requireProperty`
5. `ResidenceManagementMapper.existsActivePerson`
6. `AccessControlMapper.selectCommunityIdForProperty`
7. `ResidenceManagementMapper.selectPropertyForClaim`
8. `ResidenceManagementMapper.claimProperty`
9. `ResidenceManagementMapper.insertResidence`
10. `ResidenceManagementMapper.insertParkingEligibility`（在后端异常日志中定位为失败点）

现行 Controller 通过 `ResidenceManagementService` 接口依赖服务，而非直接依赖实现：`backend/src/main/java/com/smartproperty/platform/residency/controller/ResidenceManagementController.java:3`。Mapper SQL 位于 `backend/src/main/resources/mapper/residency/ResidenceManagementMapper.xml:8`。

---

## 6. 失败 SQL、数据库约束与精确根因

失败 SQL：

```sql
INSERT INTO residence_parking_eligibility
    (residence_id, eligible, reason_code, remark)
VALUES (?, ?, ?, ?)
```

MySQL 原始错误：

```text
Check constraint 'ck_residence_parking_eligibility_reason' is violated.
SQL state [HY000]; error code [3819]
```

约束定义位于 `database/migrations/V8__parking_permit_management.sql:6–17`：

```sql
reason_code IS NULL OR reason_code IN
('OWNER_NOT_GRANTED', 'NO_AVAILABLE_QUOTA',
 'LEASE_EXCLUDES_PARKING', 'OTHER')
```

因此该次请求已经通过了 JSON 反序列化和基础 DTO 绑定，也走到了实际 INSERT；被拒绝的是 `reason_code` 的**值域**。Mapper 的列顺序并无错误。历史日志故意省略 SQL 绑定参数，故只能证明其值不在上述集合中，不能证明具体非法原文。

---

## 7. 事务边界与回滚判定

现行服务的同一业务方法明确标有 `@Transactional`：

`backend/src/main/java/com/smartproperty/platform/residency/service/impl/ResidenceManagementServiceImpl.java:6`

历史堆栈还记录了：

```text
TransactionAspectSupport.invokeWithinTransaction
TransactionInterceptor.invoke
```

失败顺序为：

```text
claimProperty                  affected=1
insertResidence                affected=1
insertParkingEligibility       CHECK 失败 / SQLException
异常上抛至 GlobalExceptionHandler
```

`UncategorizedSQLException` 是未被业务代码吞掉的运行时数据访问异常；按照 Spring 默认事务语义，事务会回滚。日志没有开启 JDBC transaction DEBUG，因此没有一行字面量 `ROLLBACK`；本报告不把“没有那一行日志”误说成负面证据，而是结合第 8 节的当前数据库快照做验证。

**事务不包括新增人员。** `createPerson` 在 16:35:55 已经是独立的 HTTP 200 请求，`moveIn` 于 16:36:06 才开始。因而“人员仍存在”是设计正常结果，并不能否定入住事务回滚。

---

## 8. 失败后的数据库残留：只读核验

本次只执行 SELECT。针对日志中的 `person_id=1028`，当前 `community_db` 快照结果为：

| 核验项 | 结果 |
|---|---|
| `residence` 记录数 | 1 |
| 当前有效入住数（`end_date IS NULL`） | 1 |
| 唯一入住 ID | 1045 |
| 该入住房产 | 478 |
| `residence_parking_eligibility` 记录数 | 1 |
| 资格创建时间 | 2026-08-30 16:38:54.286 |
| `eligible / reason_code` | `1 / NULL` |
| 房产当前状态 | `TENANT_OCCUPIED`，version 1 |

这条资格记录的创建时间与后续成功请求 `16:38:54.292` 一致，而不是失败请求 `16:36:06`。并且失败后该人员仍能查询可办理房产、之后成功完成同一类入住；这共同说明失败事务留下的 `claimProperty` 与 `insertResidence` 未持久化。没有执行任何 DELETE 或人工清理。

注意：当前数据库只能证明最终状态；“回滚发生”的强证据是最终状态、日志顺序与 `@Transactional` 语义的组合，而不是单独某一条 SELECT。

---

## 9. 当前最小修复状态（只读确认）

现行代码已完成的局部修复：

- 页面将“不具备资格原因”由自由输入改为受控下拉：`MoveInView.vue:10–15, 40`；
- false 分支不选原因时在前端提示；
- 服务层在任何房产/入住写入前校验：缺失原因返回 `PARKING_INELIGIBILITY_REASON_REQUIRED`，非法原因返回 `PARKING_INELIGIBILITY_REASON_INVALID`；
- true 分支防御性将 `reason_code` 归一为 `NULL`；
- 未修改 V8、未新建 V9、未放宽 MySQL CHECK。

这正是 UI 中文展示值与数据库稳定机器码分离后的契约。

---

## 10. 为什么 204 不是失败或“回滚信号”

`POST /api/v1/dev/client-events` 是前端行为/错误观测接口。前端在业务请求已经收到 500 后，另发一条事件上报：

```text
业务：POST /api/admin/residence-management/move-ins → 500
观测：POST /api/v1/dev/client-events             → 204
```

HTTP 204 的含义是“观测事件已成功接收、无响应正文”。它既不代表入住成功，也不会触发或覆盖事务回滚。相同模式也出现在 16:35:10、16:35:45 的独立 409 冲突后；那些 409 是“人员 280 已有当前入住”的另一类唯一约束问题，不能与本次 MySQL 3819 混为同一异常。

---

## 11. GlobalExceptionHandler 的当时表现

当时的 `UncategorizedSQLException` 未落入 `DataIntegrityViolationException` 专用分支，最终走到了全局兜底 `Exception` 处理，因此用户看见：

```text
系统暂时无法完成操作，请稍后重试
```

对应 `backend.log:46–54` 中的 `GlobalExceptionHandler trace=503596760319 - unhandled request failure`。这解释了为何页面是 500 通用文案，而非直接暴露数据库 CHECK 细节。

修复后的服务层把非法原因提前转换为 400 业务错误，避免再让此类前端契约错误撞到 MySQL 后表现为 500。

---

## 12. 可用于 PPT 的证据强度与 Outcome 判定

### Outcome A：不成立

不把它定为“新增人员、房产状态、入住、停车资格全部一键回滚”的端到端案例。人员创建在另一条 HTTP 200 请求中，必然不会随 `moveIn` 失败回滚。

### Outcome B：成立（推荐）

这是一条真实、可讲的**入住事务内部回滚**证据链：

- 真实历史 HTTP 500；
- 真实 MySQL 3819 与命名 CHECK；
- 真实先写房产、再写入住、最后停车资格失败；
- 真实 Spring transaction interceptor 栈；
- 后续重新办理成功；
- 当前数据库只保留成功时刻的一条入住与一条资格记录。

### Outcome C：不需要

不需要重新制造失败记录，也不需要创建无害演示。已有真实日志足够，且本轮明确不重现。

---

## 13. 建议截图（不重现、不暴露个人信息）

建议在本机 IDE 打开历史日志后截取两张图：

1. **“失败证据”**：`backend.log:46–54`，辅以 `access.log:587–603`。标题可写“停车资格原因不符合数据库约束，入住事务失败”。注意遮住绝对工作目录和任何无关个人信息。
2. **“事务恢复与成功重试”**：`access.log:607–625`，以及本报告第 8 节所列只读结果（可做成 PPT 表格，不要截图数据库凭据）。标题可写“修复后以标准原因契约完成入住”。

若只保留一张图，优先第一张：它最直接展示了错误位置、约束名和 SQL 类型。不要展示 `local.env`、MySQL 命令、Authorization、JWT 或任何配置密钥。

---

## 14. P28 推荐表述

**标题：** 入住办理事务：数据库约束阻止半完成状态

**可讲述版本：**

> 在“租赁入住 + 暂不允许申请车证”分支中，旧页面把自由文本当作停车资格原因代码提交。系统先尝试更新房产占用状态并写入入住记录，随后数据库校验发现原因代码不属于允许集合，拒绝写入停车资格记录。该异常发生在同一个 Spring 事务内，因此本次入住操作未形成半完成的房产占用或入住数据。我们随后将中文展示与机器码分离，并在服务层提前校验；修复后按正常业务流程完成入住。

页脚可加一行小字：

> 新增人员为先前独立请求，不属于本事务；本页讨论的是入住事务内部回滚。

---

## 15. 对 PPT 叙述的边界建议

应强调：数据库约束与服务层校验是两道防线，服务层提供可理解的 4xx 反馈，数据库确保非法状态无法写入。演示重点是“事务边界清楚、失败不留下半完成入住”，不是炫耀异常堆栈。

推荐以流程图表达：

```text
房产状态更新 → 入住记录插入 → 停车资格校验失败
       └──────────────── Spring @Transactional 回滚 ────────────────┘
```

并在流程图外单列“新增人员（独立 200 请求）”，避免课堂上被追问“为什么 person 还在”。

---

## 16. Do Not Claim（不得在 PPT 或答辩中声称）

- 不要说这是“JSON 格式错误”或 JSON 无法反序列化；证据表明请求已到 MySQL INSERT。
- 不要说那次具体输入值就是“刘”或“刘刘”；历史日志未记录绑定参数。
- 不要说 204 是业务失败、事务回滚或 200 被覆盖；204 仅是前端观测上报成功。
- 不要把 16:35 的 409（人员已有当前入住）与 16:36 的 500（停车资格 CHECK）当作同一故障。
- 不要说“新增人员也被回滚”；它早于入住请求并已独立成功。
- 不要说日志中有一行字面 `ROLLBACK`；事务调试日志未开启。
- 不要说失败使用的房产 ID 已由日志直接揭示；历史 Flow 为安全起见隐藏了绑定参数。当前 ID 478 是后续成功记录的只读快照。
- 不要把当前数据库快照单独当作历史回滚的唯一证据；它应与原始日志和事务边界一并说明。

## 审计范围确认

本报告是本轮唯一新增文件。未修改 PowerPoint、Java、Vue、SQL、迁移、数据库数据、启动脚本或日志；未执行错误重现。
