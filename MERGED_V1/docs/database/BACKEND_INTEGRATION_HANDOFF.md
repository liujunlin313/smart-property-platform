# BACKEND_INTEGRATION_HANDOFF · 第二阶段数据库交接

> 本文件只定义未来 Spring Boot 边界；Database Phase 1 不包含任何后端业务代码。
>
> 历史版本说明：本文的约束映射以 V1 为起点。Phase 2A / V3 已把产权规则改为单一完整产权人，当前实现应映射 `ck_ownership_single_owner_share` 与 `uk_ownership_one_current_owner`。

## 1. 唯一迁移来源

`database/migrations/` 是当前唯一可信来源。第二阶段接入 Flyway 时必须直接使用或迁移该目录的所有权，禁止另复制一份内容不同的 `schema.sql`。

若迁移目录最终移动到 `backend/src/main/resources/db/migration/`，应通过一次明确的仓库变更完成，并删除旧来源；不允许长期双写。

## 2. 表类型

核心实体表：Community、Building、BuildingUnit、PropertyUnit、LayoutTemplate、Person、Agency、RentalListing、LeaseContract、FeeType、FeeStandard、Meter、MeterReading、Bill、Payment、ParkingSpace、Vehicle、FacilityType、FacilityPlacement。

关系/关联表：Ownership、Residence、Authorization、ContractParticipation、ParkingRight。

弱实体：BillItem，必须通过 Bill 聚合根管理。

## 3. 禁止随意直接修改

- Bill、BillItem、Payment：财务与支付历史；正式后不得普通 CRUD 覆盖。
- LeaseContract、ContractParticipation：合同聚合；参与方和状态应统一在事务中处理。
- Ownership、Residence、Authorization、ParkingRight：时态关系；以新增版本/结束区间为主，不直接覆盖历史。
- MeterReading：抄表事实；修正应保留审计语义。
- FeeStandard：费率版本；新标准新增一行并关闭旧区间。

## 4. 事务边界

后续 Service 至少需要以下事务：

1. 创建/变更产权：锁定相关有效 Ownership，校验份额和区间，写入新版本。
2. 建立中介授权：确认授权人当前产权和权限区间，再插入 Authorization。
3. 发布中介房源：锁定并复核有效 Authorization；AGENCY Listing 使用复合 FK 保证同一 PropertyUnit。
4. 房源成交：锁定 Listing，创建唯一 LeaseContract、写入多个 ContractParticipation、更新 Listing 状态。
5. 出账：选定有效 FeeStandard 和 MeterReading，写 Bill/BillItem；BillItem 金额由数据库生成。
6. 支付：锁定 Bill，校验应收与已成功支付合计，写 Payment，更新 Bill 状态。
7. 车位权变更：锁定 ParkingSpace 的有效权利区间，防止排他权重叠。

## 5. 建议 REST 资源边界

- `/api/v1/communities`
- `/api/v1/buildings`
- `/api/v1/building-units`
- `/api/v1/properties`
- 后续：`/people`、`/ownerships`、`/residences`、`/authorizations`、`/rental-listings`、`/lease-contracts`、`/bills`、`/payments`、`/parking-spaces`。

BillItem、ContractParticipation 通常作为聚合根的子资源，不提供脱离 Bill/LeaseContract 的任意 CRUD。

## 6. 用户输入与系统生成

可由用户输入：业务名称/编号、地址、面积、户型枚举、日期、联系方式、坐标、房源文案、授权权限、支付方式等。

由系统生成：所有 surrogate ID、created_at/updated_at、BillItem.amount、账单号/支付号/合同号（由受控编号服务生成）、正式状态流转时间。

敏感字段：id_number 不得完整返回列表接口；日志不得输出完整证件号、数据库密码或未来 JWT。

## 7. 数据库错误到业务错误的转换

| DB 约束 | 未来业务错误示例 |
|---|---|
| `uk_building_community_no` | `BUILDING_NUMBER_DUPLICATE` / 同一小区楼栋号已存在 |
| `uk_building_unit_building_no` | `BUILDING_UNIT_NUMBER_DUPLICATE` |
| `uk_property_unit_parent_no` | `PROPERTY_NUMBER_DUPLICATE` |
| FK 父层级不存在 | `PARENT_RESOURCE_NOT_FOUND` |
| `ck_ownership_single_owner_share` | `OWNERSHIP_SHARE_MUST_BE_FULL` |
| `uk_ownership_one_current_owner` | `PROPERTY_ALREADY_HAS_CURRENT_OWNER` |
| 日期 CHECK | `EFFECTIVE_PERIOD_INVALID` |
| `uk_lease_contract_listing` | `LISTING_ALREADY_CONTRACTED` |
| Listing-Authorization 复合 FK | `AUTHORIZATION_PROPERTY_MISMATCH` |
| `ck_facility_mobility` | `FACILITY_MOVEMENT_RULE_INVALID` |

后端不得把原始 SQL、表结构或堆栈直接返回浏览器。

## 8. 查询与 Mapper 提示

- 房产列表沿 PropertyUnit→BuildingUnit→Building→Community JOIN，只投影页面需要列。
- 当前关系统一使用 `start_date <= :date AND (end_date IS NULL OR end_date >= :date)`。
- 存在性问题优先考虑 EXISTS/NOT EXISTS；避免为了判断存在而产生重复行。
- 账单项与支付分别预聚合后再 JOIN，避免两个 1:N 同时 JOIN 导致金额倍增。
- 最新抄表使用 `ROW_NUMBER() OVER (PARTITION BY meter_id ORDER BY reading_at DESC)`。
- 后续复合搜索先下推 Community/状态/价格等选择条件，并尽早投影必要列；最终用 EXPLAIN 验证。

## 9. 下一阶段开工 Gate

只有人工确认 “Database Phase 1 PASS” 后，才创建 Spring Boot/Vue 工程。接入时先验证 Flyway 能在空库执行既有迁移，再开始 Community→Building→BuildingUnit→PropertyUnit 纵向链。
