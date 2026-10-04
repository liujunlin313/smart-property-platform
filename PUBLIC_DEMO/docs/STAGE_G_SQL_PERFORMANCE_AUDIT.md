# Stage G SQL 性能与 N+1 审计

## 审计方法

对 Mapper XML 与服务调用链筛选了 10 类核心查询，再以真实 `community_db` 执行 `SHOW INDEX`、`EXPLAIN`、`EXPLAIN ANALYZE`。时间属于本机数百行演示数据的瞬时采样，只用于证明访问路径，不作为生产吞吐结论。

| 编号 | 查询路径 | 执行计划证据 / 判断 | 本轮处理 |
| --- | --- | --- | --- |
| SQL-01 | `AccessControlMapper.selectActiveCommunityScopeIds` | `PRIMARY(account_id,community_id)` index lookup；2 rows，约 0.016ms | 已合理，不加索引。 |
| SQL-02 | 当前房产的 Residence / Person 查询 | `idx_residence_property_unit` lookup；2 rows，约 0.033ms | 已合理。 |
| SQL-03 | 账单按账期、状态后关联 `bill_item` | 优化前 `bill` PK scan：603 rows → 11 rows，约 0.85ms | 新增 V6。 |
| SQL-04 | 同 SQL-03 优化后 | `idx_bill_period_status` lookup：11 rows；约 0.13ms；再经 `bill_item` PK lookup | 已改善；不夸大为生产倍率。 |
| SQL-05 | 通知发送记录分页 | `idx_notification_sender_sent` reverse index lookup；6 rows，约 0.04ms | 已合理。 |
| SQL-06 | 按 person 读通知收件箱 | `idx_notification_recipient_person_read` lookup；3 rows，约 0.04ms | 已合理；排序仅对命中的极小集合。 |
| SQL-07 | 小区 → 楼栋 → 单元 → 房产 | `uk_building_community_no`、`uk_building_unit_building_no`、`uk_property_unit_parent_no` 连续 lookup；72 rows，约 0.14ms | 已合理。 |
| SQL-08 | 房产分页 / Count | 阅读 `PropertyMapper.xml`：先按层级关联并按 community/building/unit/property 排序 | 当前规模没有计划显示热点；不盲加宽索引。 |
| SQL-09 | 通知目标搜索 | `NotificationMapper.searchTargetOptions` 使用分层 `UNION ALL` 与受控 `limit≤50`；空关键字只取社区根 | LIKE 搜索以后可考虑全文/专用搜索策略；本轮无证据建索引。 |
| SQL-10 | Dashboard / 月年统计 / Rental 多条件筛选 | 多数为聚合、视图或小数据集读取；未发现不合理显式锁或可复现瓶颈 | 保持真实聚合逻辑。 |

## SQL-03 Before / After

待优化业务语义：选择“2026 年 7 月、OVERDUE”的账单，再按 Bill 主键关联收费项目。

| 项目 | Before | After (V6) |
| --- | --- | --- |
| `bill` access | PK index scan | `idx_bill_period_status` index lookup |
| 参与过滤行 | 603 → 11 | 11 |
| `bill_item` | 每个 Bill 继续走 PK | 不变，仍走 PK |
| 实测总时间 | 约 0.85ms | 约 0.13ms |

新增迁移：`database/migrations/V6__stage_g_bill_period_status_index.sql`。该迁移不改业务数据，不改 V1～V5，并已使用项目独立 Flyway 运行器登记为版本 6。

## 收费通知 N+1 结论

不存在“每个 Person 单独查欠费”的 N+1：`NotificationServiceImpl` 将全部收件人 ID 交给 `ChargeNoticeComposer.compose`，后者仅调用一次 `ChargeNoticeMapper.selectDueRows(personIds, scope)`，然后在 Java 中按 `personId` 分组并生成每人 payload。预览日志直接复用已生成的 payload，不增加任何 SQL。

## DataScope 重复读取观察

非系统管理员的每次受保护 HTTP 请求，当前会读取权限集合，并在需要范围时读取 `account_community_scope`。同一页面并行发出 5～10 个 API 时会重复这些稳定查询；Flow 日志已可见一次请求的权限 SQL 与 Scope SQL。单条 Scope 查询当前约 0.016ms、仅返回 1～2 行，短期不构成瓶颈。

下一阶段若需缓存，建议以 `accountId → {roles, permissions, communityIds}` 作为 Redis `AuthorizationSnapshot`，短 TTL（例如 60 秒），在角色或 Scope 写入时主动逐出。该建议本轮仅记录，未改变 Redis 或鉴权行为。
