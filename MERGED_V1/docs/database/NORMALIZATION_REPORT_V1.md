# NORMALIZATION_REPORT_V1 · 函数依赖与范式分析

## 1. 分析方法

本报告同时检查物理主键和业务候选键。使用代理主键不能成为跳过 2NF/3NF 分析的理由；对 Ownership、Residence、Authorization、ContractParticipation、ParkingRight、BillItem 仍按其自然/业务复合键检查。

## 2. 1NF

所有字段均为单值原子属性：

- Person 不保存 `properties="101,202"` 或 `roles="业主,租户"`。
- PropertyUnit 不保存逗号分隔的设施；一件设施实例是一行 FacilityPlacement。
- LeaseContract 不保存多个人员 ID 列表；参与人拆到 ContractParticipation。
- Bill 不保存账单项目集合；一项费用是一行 BillItem。
- 户型类型是一个有限枚举值；没有把卧室/客厅拆成 Room 集合。

因此所有关系满足 1NF。

## 3. 重点复合关系的函数依赖

### 3.1 Ownership

业务候选键：`(person_id, property_unit_id, start_date)`。

```text
(person_id, property_unit_id, start_date)
    → ownership_share, end_date, certificate_no
```

份额和有效期不由 person_id、property_unit_id 或其任意二元子集单独决定。人员姓名依赖 Person，房产地址依赖 PropertyUnit 的空间父链，均未复制到 Ownership。因此满足 2NF 和 3NF。

### 3.2 Residence

业务候选键：`(person_id, property_unit_id, start_date)`。

```text
(person_id, property_unit_id, start_date)
    → residence_role, end_date
```

Residence 只表达实际居住；ownership_share、产权证明等属性不属于该 FD，也没有被混入。非键属性依赖完整业务键，无传递依赖，满足 3NF。

### 3.3 Authorization

业务候选键：`(person_id, property_unit_id, agency_id, start_date)`。

```text
(person_id, property_unit_id, agency_id, start_date)
    → end_date, status,
      can_view, can_publish, can_edit_listing, can_manage_contract
```

- `(person_id, agency_id)` 不能决定具体房产；
- `(property_unit_id, agency_id)` 不能决定授权业主；
- `(person_id, property_unit_id)` 不能决定哪家机构。

授权属性依赖完整三元联系及其版本起点，满足 2NF。Agency 名称、Person 姓名和房产地址留在各自关系，消除传递依赖，满足 3NF。

### 3.4 ContractParticipation

候选键/主键：`(lease_contract_id, person_id, participant_role)`。

```text
(lease_contract_id, person_id, participant_role)
    → is_primary, signed_at
```

同一人理论上可能以不同合法角色参与不同业务语境，故 role 是键的一部分。签署时间与主要联系人标识依赖完整参与事实，不把合同或人员属性复制进来，满足 2NF/3NF。

### 3.5 ParkingRight

业务候选键：`(person_id, parking_space_id, start_date)`。

```text
(person_id, parking_space_id, start_date)
    → right_type, end_date, status
```

权利属性不由 Person、ParkingSpace 或二者在无时间版本时单独决定。车辆信息未进入本关系，避免 `person → vehicle` 的无关依赖，满足 3NF。

### 3.6 BillItem（弱实体）

候选键/主键：`(bill_id, item_no)`。

```text
(bill_id, item_no)
    → fee_standard_id,
      previous_reading_id, current_reading_id,
      quantity, unit_price, note
(quantity, unit_price) → amount  （数据库生成列）
```

`item_no` 只在某张 Bill 内有意义，BillItem 不能脱离 Bill 被识别。费种名称不重复保存；`fee_standard_id → fee_type_id` 通过 JOIN 推导，所以 BillItem 不再保存 fee_type_id，避免传递/重复依赖。amount 由数据库生成并保持一致。满足 2NF/3NF。

## 4. 其他关键 FD 与拆分

### 空间资产

```text
community_id → community_code, community_name, address_line, ...
building_id → community_id, building_no, building_name, ...
building_unit_id → building_id, unit_no, ...
property_unit_id → building_unit_id, layout_template_id, property_no, ...
```

若把 community_name 放入 PropertyUnit，将出现：

```text
property_unit_id → building_unit_id → building_id → community_id → community_name
```

这是可避免的传递依赖和更新异常，所以只在查询时 JOIN。

### 房产/房源/合同

```text
property_unit_id → 房产静态属性
rental_listing_id → 一次挂牌属性
lease_contract_id → 成交合同属性
```

若把 monthly_rent/status/published_at 放进 PropertyUnit，同一房产第二次挂牌会覆盖第一次历史；拆分后每个生命周期事实依赖自己的键。

### 收费链

```text
(community_id, fee_type_id, effective_from) → unit_price, effective_to
(meter_id, reading_at) → reading_value, reading_source
(property_unit_id, billing_year, billing_month) → bill_id, due_date, status
```

FeeStandard 与 MeterReading 分离，使“当期使用量来自何处、采用哪版收费标准”均可追溯。

## 5. 3NF 结论

25 个 V1 关系均达到至少 3NF。多数实体字典关系同时满足 BCNF。关键依据是：

1. 集合/多值事实拆为关系，满足 1NF；
2. 复合关系的非键属性依赖完整业务键，满足 2NF；
3. 人员名、小区名、费种名、地址等依赖其他实体键的属性不在关系表重复，满足 3NF。

## 6. 有意识保留的历史快照

以下字段不是无意冗余，而是不可随当前配置变化的历史事实：

- `lease_contract.monthly_rent/deposit_amount`：签署时合同金额快照；Listing 后续关闭或修订不能改变合同。
- `bill_item.unit_price`：出账时实际采用的单价快照；FeeStandard 后续版本变化不能重写历史账单。

一致性方式：这些字段只能在“形成合同/出账”事务中写入；正式状态后不可任意修改。

## 7. 数据库约束与 Service 规则的边界

数据库直接保证：PK、FK、UNIQUE、单行 CHECK、生成金额、BillItem 识别依赖、Listing 与 Authorization 的房产一致性。

以下跨行/跨表/时态聚合规则留给后续 Service 事务：

- 同一房产某时点有效产权份额总和不超过 1；
- Ownership/Residence/Authorization/FeeStandard/ParkingRight 区间重叠策略；
- Authorization 的 Person 在授权期内确为该房产产权人；
- Meter 的 FeeType 必须是 METERED，读数不倒退；
- BillItem 的标准所属小区、读数表具、房产和费种必须因果一致；
- 成功 Payment 合计不超过应收，并同步 Bill 状态；
- 正式 LeaseContract 至少一名 LANDLORD 和一名 TENANT。

把这些规则全部硬塞入 CHECK 不可行；用触发器提前实现也会削弱后续 Service 事务边界和课程阶段中的触发器实验。

## 8. V1 与后续优化

范式正确性优先于提前冗余。`bill.total_amount`、统计汇总表、搜索文本、空间 POINT 均未在 V1 直接加入。后续只有在保存数据规模、SQL、EXPLAIN ANALYZE、扫描行数与执行时间证据后，才讨论受控冗余或高级索引。

