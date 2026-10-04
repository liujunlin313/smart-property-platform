# Stage UI-R2 Hotfix-2：租赁入住停车资格原因契约修复报告

## 范围

本次仅修复“租赁入住 + 暂不允许申请车证”分支的资格原因契约。未重构入住、车证、权限、DataScope 或数据库结构。

## 修改前审计结论

### V8 的真实定义

`residence_parking_eligibility` 的相关字段为：

| 字段 | 定义 | 是否可空 |
|---|---|---|
| `eligible` | `TINYINT(1)` | 否；CHECK 限制为 `0` 或 `1` |
| `reason_code` | `VARCHAR(32)` | 是 |
| `remark` | `VARCHAR(300)` | 是 |

`ck_residence_parking_eligibility_reason` 的完整逻辑是：

```sql
reason_code IS NULL OR reason_code IN (
  'OWNER_NOT_GRANTED',
  'NO_AVAILABLE_QUOTA',
  'LEASE_EXCLUDES_PARKING',
  'OTHER'
)
```

因此，V8 本身不把 `eligible` 与 `reason_code` 做交叉绑定；它只限制原因必须为 `NULL` 或以上四个稳定机器码。应用层的业务契约为：允许申请时统一写入 `NULL`；暂不允许时必须选择四个枚举中的一个。

### 原始调用链

- 页面 `MoveInView.vue` 的“不具备资格原因”是自由文本输入框。
- 页面提交时把 `parkingIneligibleReasonCode` 原样发送。
- `MoveInRequest` 无枚举校验；Service 仅检查 false 分支是否非空，也原样写入 Mapper。
- Mapper 的字段顺序已经正确：`residence_id, eligible, reason_code, remark`。

失败日志没有展开 SQL 绑定参数，因此不能安全、准确地重述用户输入的原始文字；但它在 Service 的“非空”检查后到达 MySQL，且被 V8 CHECK 拒绝，已证明其不是四个允许机器码之一。

### 失败事务回滚

失败发生在 `insertParkingEligibility`。访问日志显示同一位新建人员（1028）在失败后仍可查询到可办理房产，随后以允许申请分支完成成功入住，说明前一次的房产占用与入住插入均已回滚。当前该人员仅有后续成功请求创建的一条有效入住与资格记录；未进行任何人工删除。

## 最小修复

1. `frontend/src/views/residency/MoveInView.vue`
   - 将 false 分支的自由文本原因输入改为受控下拉选择；中文 `label` 与 V8 机器码 `value` 分离。
   - 提交前缺少原因时提示“请选择不允许申请车证的原因”。
   - 切回“允许申请”时清空原因，保证发送 `NULL`。
2. `backend/.../ResidenceManagementServiceImpl.java`
   - 复用现有 `BusinessException`，在任何房产/入住写入前校验 false 分支原因。
   - 缺失原因返回 `400 PARKING_INELIGIBILITY_REASON_REQUIRED`。
   - 非法机器码返回 `400 PARKING_INELIGIBILITY_REASON_INVALID`。
   - true 分支防御性归一化为 `reason_code = NULL`。

没有修改 Request DTO、Mapper SQL、V1–V8 或数据库 Schema；没有创建 V9。

## 验证

- 后端 `compile`：PASS
- 前端 `npm run build`：PASS（仅现有第三方构建警告）

### Smoke 1：允许申请

Person 1026、Property 569：HTTP 200，创建 Residence 1046。

数据库核验：`eligible=1`、`reason_code=NULL`、房产状态 `TENANT_OCCUPIED`。

### Smoke 2：暂不允许申请

Person 1025、Property 568，选择 `LEASE_EXCLUDES_PARKING`：HTTP 200，创建 Residence 1047。

数据库核验：`eligible=0`、`reason_code=LEASE_EXCLUDES_PARKING`、备注正确保存、房产状态 `TENANT_OCCUPIED`。

上述是本轮经授权产生的合法测试业务记录，未清理或删除。

## 运行环境

两条 Smoke 使用临时后端端口 8081，测试结束后已精确停止该 Java/Maven 进程并确认 8081 释放。未终止任何现有应用进程；复核时 8080 和 5173 均无监听进程。

## Deferred

无。本轮不涉及其他模块。
