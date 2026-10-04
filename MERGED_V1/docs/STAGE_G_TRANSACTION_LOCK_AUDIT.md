# Stage G 事务、MVCC 与锁审计

## 数据库隔离级别

实际查询 `SELECT @@transaction_isolation` 的结果为：`REPEATABLE-READ`。InnoDB 的普通 SELECT 在该隔离级别下应保持一致性读 / MVCC；本轮没有为了“看起来更安全”给列表、统计或通知快照查询盲目增加 `FOR UPDATE`。

## 业务审计结论

| 路径 | 当前事务 / 锁 | 结论 |
| --- | --- | --- |
| 登录 Refresh Token 轮换 | `AuthService.refresh` 为事务；`AuthMapper.findRefreshSessionForUpdate` 使用 `FOR UPDATE` | 合理。它保护同一 Refresh Token 的一次性轮换，避免并发重复使用。 |
| 登录失败/成功次数 | 认证服务内事务更新账号计数 | 未见需要额外全表锁的理由。 |
| Notification + NotificationRecipient | `NotificationServiceImpl.send` 为一个事务，先写通知再批量写收件人 | 合理。父记录与收件人一起提交；预览仅在 commit 后写日志。 |
| 收费通知欠费读取 | 单次批量 `selectDueRows`，无 `FOR UPDATE` | 合理。它是“发送当刻的账务通知快照”，不是扣款交易；应依赖 MVCC。 |
| Payment / Bill 状态更新 | 现有支付、账单写操作需要在后续真实并发支付场景单独压测 | 当前没有证据表明应给普通统计读加锁。若将来出现双重支付，应以具体更新条件、幂等号、乐观版本或最小行锁审计，而不是预先加锁。 |
| Ownership / Residence 变更 | 当前通过关系表和业务唯一约束保持历史关系 | 对并发变更应在对应写服务中按实际冲突路径审计；本轮不改变既有规则。 |
| Property / Resident / Dashboard / Map 查询 | 普通 SELECT | 保持 MVCC consistent read，不加 `FOR UPDATE`。 |

## 发现与后续项

- 已确认唯一显式 `FOR UPDATE` 位于 Refresh Token Session 轮换路径，属于合理的行级并发控制。
- 未发现收费通知批量查询需要锁定 Bill 的证据；锁住账单会无端阻塞支付/收费处理。
- 本轮没有修改隔离级别、没有新增行锁、没有更改支付或账单状态规则。
- 后续若增加真实支付回调，应以“同一支付流水重复到达”“同一 Bill 并发部分支付”两类场景建立专门事务测试，再选择悲观锁或乐观锁策略。
