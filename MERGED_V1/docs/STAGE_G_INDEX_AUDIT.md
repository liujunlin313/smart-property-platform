# Stage G 核心索引审计

审计基于 `community_db` 的 `SHOW INDEX` 与实际 `EXPLAIN ANALYZE`。这里的“索引”按用途说明，不把同一约束产生的多列统计为虚假的多项优化。

在本报告覆盖的 20 张核心表中，实测共有 69 个索引对象：20 个主键索引和 49 个非主键（唯一/普通）索引。这个数量仅是盘点，不作为“索引越多越好”的评价依据。

| 表 | 主键 / 唯一约束 | 关键业务索引 | 结论 |
| --- | --- | --- | --- |
| `app_account` | PK(account_id)，UK(username)，UK(person_id) | — | 登录和 Person 绑定均被唯一键覆盖。 |
| `account_global_role` | PK(account_id, role_id) | role_id 外键索引 | 账号取角色走主键前缀。 |
| `account_community_scope` | PK(account_id, community_id) | community_id 外键索引 | Scope 查询按 account_id 命中 PK 前缀。 |
| `residence` | PK(residence_id)，UK(person_id, property_unit_id, start_date) | `idx_residence_property_unit`，当前住户生成列唯一键 | 当前住户、房产到住户均有路径。 |
| `ownership` | PK(ownership_id)，业务唯一约束 | property_unit_id、当前产权生成列唯一键 | 产权人/房产关系可直接定位。 |
| `property_unit` | PK，UK(building_unit_id, property_no) | layout_type_id | 层级查询使用 parent-no 唯一键。 |
| `building_unit` | PK，UK(building_id, unit_no) | — | 建筑到单元走唯一键。 |
| `building` | PK，UK(community_id, building_no) | — | 小区到楼栋走唯一键。 |
| `community` | PK，UK(community_code) | — | 小区基本表规模小。 |
| `bill` | PK，UK(property_unit_id, billing_year, billing_month) | **V6 `idx_bill_period_status(billing_year,billing_month,status,bill_id)`** | 账期+状态通知/催缴筛选已有专用路径。 |
| `bill_item` | PK(bill_id,item_no) | fee_standard_id / 读数外键索引 | Bill 到项目走主键。 |
| `payment` | PK(payment_id)，UK(payment_no) | bill_id、payer_person_id 等外键索引 | 保留，未重复建 bill_id+status。 |
| `fee_standard` | PK | community_id、fee_type_id | 适配规则查询。 |
| `fee_type` | PK，UK(fee_code) | — | 小型字典表。 |
| `notification` | PK | `idx_notification_sender_sent(sender_account_id,sent_at)` | 已覆盖发件箱逆序分页。 |
| `notification_recipient` | PK，UK(notification_id,person_id) | `idx_notification_recipient_person_read` | 不重复添加 notification/person 组合索引。 |
| `rental_listing` | PK | property_unit_id、agency_id、status 等现有索引 | 当前筛选规模下未见明确瓶颈。 |
| `authorization` | PK | property_unit_id、agency_id 等现有索引 | 维持现状。 |
| `meter` / `meter_reading` | 各自 PK/业务唯一约束 | 关联外键索引 | 未见本轮查询计划显示的热点全扫。 |

## 仅新增一条索引的理由

V6 新增 `bill(billing_year, billing_month, status, bill_id)`。优化前，实际账期+状态筛选扫描 `bill` 主键约 603 行后过滤为 11 行；优化后直接以 `idx_bill_period_status` 取到 11 行。其余候选（`notification_recipient(notification_id,person_id)`、`account_community_scope(account_id,community_id)`、`residence(property_unit_id,...)`）均已有同等或更好的 PK/UK/索引覆盖，因此没有重复创建。
