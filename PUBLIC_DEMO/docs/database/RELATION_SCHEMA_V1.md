# RELATION_SCHEMA_V1 · 正式关系模式设计

> 历史版本说明：本文记录 Database Phase 1 / V1 基线。Phase 2A 的 V3 迁移已按最终业务决定将产权收紧为“每套房同一时刻仅一个产权人，`ownership_share = 1`”；当前实现以迁移目录中的最新版本为准。

版本：Database V1  
数据库：MySQL 8.0+ / InnoDB / utf8mb4  
物理表：25 张  
迁移唯一来源：`database/migrations/`

## 1. 冻结设计结论

- 空间资产严格采用 `Community → Building → BuildingUnit → PropertyUnit`。
- `PropertyUnit`（房产单元）是最小产权、居住、出租和主要收费单位，不建 `Room`。
- `Ownership`（产权）与 `Residence`（实际居住）分别建表，不用 `Occupancy` 合并。
- `Authorization` 同时引用 Person、PropertyUnit、Agency，是三元联系的关系化结果。
- PropertyUnit、RentalListing、LeaseContract 分别表达房产、一次挂牌生命周期和成交合同。
- 合同人员通过 `ContractParticipation` 表达，可有多名出租方和多名承租方。
- 收费链包含 FeeType、FeeStandard、Meter、MeterReading、Bill、BillItem、Payment。
- `BillItem` 是弱实体，识别键为 `(bill_id, item_no)`。
- `ParkingRight` 的权利主体是 Person；Vehicle 仅登记车辆。
- LayoutTemplate 描述整套房户型；FacilityPlacement 坐标始终属于一套 PropertyUnit。
- V1 不建立高级复合、覆盖或空间索引，为后续 EXPLAIN 对比保留基线。

## 2. 关系清单

| 业务域 | 关系名（物理表） | 中文名称 | 主要联系 |
|---|---|---|---|
| 空间资产 | `community` | 小区 | 1:N Building |
| 空间资产 | `building` | 楼栋 | N:1 Community；1:N BuildingUnit |
| 空间资产 | `building_unit` | 楼栋单元 | N:1 Building；1:N PropertyUnit |
| 户型 | `layout_template` | 户型模板 | 1:N PropertyUnit |
| 空间资产 | `property_unit` | 房产单元 | 产权、居住、出租、收费的共同业务对象 |
| 人员 | `person` | 人员 | 现实中的人，不等于账号或全局角色 |
| 人员房产 | `ownership` | 产权关系 | Person M:N PropertyUnit，带时态和份额 |
| 人员房产 | `residence` | 居住关系 | Person M:N PropertyUnit，带时态 |
| 租赁 | `agency` | 中介公司 | 授权业务主体 |
| 租赁 | `authorization` | 中介授权 | Person + PropertyUnit + Agency 三元联系 |
| 租赁 | `rental_listing` | 出租房源 | PropertyUnit 1:N RentalListing |
| 租赁 | `lease_contract` | 租赁合同 | RentalListing 1:0..1 LeaseContract |
| 租赁 | `contract_participation` | 合同参与 | Person M:N LeaseContract，区分角色 |
| 收费 | `fee_type` | 收费项目 | 收费类型字典 |
| 收费 | `fee_standard` | 收费标准 | Community + FeeType 的时态联系 |
| 收费 | `meter` | 计量表 | PropertyUnit 1:N Meter |
| 收费 | `meter_reading` | 抄表记录 | Meter 1:N MeterReading |
| 收费 | `bill` | 物业账单 | PropertyUnit 1:N Bill |
| 收费 | `bill_item` | 账单项目 | Bill 1:N BillItem（弱实体） |
| 收费 | `payment` | 支付 | Person 支付 Bill |
| 车位 | `parking_space` | 车位 | Community 1:N ParkingSpace |
| 车辆 | `vehicle` | 车辆 | Person 1:N Vehicle |
| 车位 | `parking_right` | 车位权利 | Person M:N ParkingSpace，带时态 |
| 户型设施 | `facility_type` | 家具/设施类型 | 设施类型字典 |
| 户型设施 | `facility_placement` | 设施放置 | PropertyUnit 上的二维坐标覆盖层 |

## 3. 户型有限枚举（中英对照）

`layout_template.layout_type` 使用 MySQL `ENUM` 固定 V1 合法值：

| English value | 中文含义 |
|---|---|
| `STUDIO` | 开间 |
| `ONE_BEDROOM` | 一居室 |
| `ONE_BEDROOM_ONE_LIVING` | 一室一厅 |
| `TWO_BEDROOM` | 两居室 |
| `TWO_BEDROOM_ONE_LIVING` | 两室一厅 |
| `TWO_BEDROOM_TWO_LIVING` | 两室两厅 |
| `THREE_BEDROOM_ONE_LIVING` | 三室一厅 |
| `THREE_BEDROOM_TWO_LIVING` | 三室两厅 |
| `FOUR_BEDROOM_TWO_LIVING` | 四室两厅 |
| `OTHER` | 其他 |

该枚举表示完整住宅户型，不代表房间实体。`template_plan_object_key` 是完整户型模板图；`property_unit.floor_plan_object_key` 可保存某套具体房产的完整户型图覆盖。两者均不是单个卧室图片。

## 4. 逐关系定义

以下 `created_at` / `updated_at` 均为系统生成审计时间，不再在每表重复解释。

### 4.1 Community（`community`，小区）

- 业务意义：多小区系统的顶层空间资产。
- 主要属性：`community_id`（小区ID）、`community_code`（业务编码）、`community_name`（名称）、`address_line`（地址）、`longitude/latitude`（经纬度）、`completion_year`（建成年份）、`status`（状态）。
- 候选键：`community_id`；`community_code`。
- 主键：`community_id`，稳定、短且不随业务编码调整。
- 外键：无。
- NULL / UNIQUE / CHECK：名称和地址非空；编码唯一；经纬度可空但分别限制在 [-180,180]、[-90,90]；状态只允许 ACTIVE/INACTIVE。
- 核心 FD：`community_id → 全部非主属性`；`community_code → 全部非主属性`。
- 范式：原子字段，单属性候选键，无部分或传递依赖，满足 3NF/BCNF。
- 历史/删除：真实小区优先改为 INACTIVE，不物理删除。
- 典型查询：小区模糊搜索、范围查询入口、小区楼栋统计。

### 4.2 Building（`building`，楼栋）

- 业务意义：小区内的一栋楼。
- 属性：`building_id`（楼栋ID）、`community_id`（小区ID）、`building_no`（楼栋编号）、`building_name`（显示名称）、`total_floors`（总层数）、`status`。
- 候选键：`building_id`；`(community_id, building_no)`。
- 主键/外键：PK=`building_id`；FK `community_id → community`。
- 规则：同一小区楼栋编号唯一；楼层数可空，非空时大于 0。
- FD：`building_id → 全部属性`；`(community_id, building_no) → building_name,total_floors,status`。
- 范式：不重复小区名称/地址，消除 `building → community → community_name` 的传递冗余，满足 3NF。
- 历史/删除：停用而非删除；父小区删除受 RESTRICT 限制。
- 查询：按小区列楼栋；房产完整地址 JOIN。

### 4.3 BuildingUnit（`building_unit`，楼栋单元）

- 业务意义：某楼栋内的入口/单元层级。
- 属性：`building_unit_id`、`building_id`、`unit_no`（单元编号）、`unit_name`、`status`。
- 候选键：`building_unit_id`；`(building_id, unit_no)`。
- PK/FK：PK=`building_unit_id`；FK `building_id → building`。
- 规则：同一楼栋单元编号唯一；核心字段非空。
- FD：`building_unit_id → 全部属性`；`(building_id,unit_no) → unit_name,status`。
- 范式：不重复 community_id 或楼栋名，满足 3NF。
- 历史/删除：状态停用，父楼栋删除 RESTRICT。
- 查询：按楼栋列单元；定位房产父层级。

### 4.4 LayoutTemplate（`layout_template`，户型模板）

- 业务意义：可由多套房采用的完整住宅户型模板。
- 属性：`layout_template_id`、`layout_code`、`layout_name`、`layout_type`（有限枚举）、`standard_area`、`template_plan_object_key`（完整户型图对象键）、`description`、`status`。
- 候选键：`layout_template_id`；`layout_code`。
- PK/FK：PK=`layout_template_id`；无 FK。
- 规则：编码唯一；类型不可空；参考面积可空但必须为正；对象键可空，为后续 MinIO 预留。
- FD：`layout_template_id → 全部属性`；`layout_code → 全部属性`。
- 范式：不存在卧室列表或设施列表等多值字段，满足 3NF。
- 历史/删除：已被房产引用的模板仅停用。
- 查询：按户型枚举筛选房产/房源、热门户型聚合。

### 4.5 PropertyUnit（`property_unit`，房产单元）

- 业务意义：最小产权、居住、出租和主要收费单位。
- 属性：`property_unit_id`、`building_unit_id`、`layout_template_id`、`property_no`（房号）、`floor_no`、`gross_area`、`usable_area`、`property_use`、`floor_plan_object_key`（具体整套房户型图）、`status`。
- 候选键：`property_unit_id`；`(building_unit_id, property_no)`。
- PK/FK：PK=`property_unit_id`；FK 到 BuildingUnit；LayoutTemplate 可空 FK。
- 规则：同一 BuildingUnit 房号唯一；建筑面积正数；套内面积可空但不大于建筑面积；用途/状态受 CHECK。
- FD：`property_unit_id → 全部属性`；`(building_unit_id,property_no) → 全部其余属性`。
- 范式：不保存 community_name、building_name、community_address；这些沿父链 JOIN 得到，满足 3NF。
- 历史/删除：真实资产只停用/归档；所有历史关系使用 RESTRICT。
- 查询：完整空间路径、按小区/楼栋筛选、空置判断、租赁与账单入口。

### 4.6 Person（`person`，人员）

- 业务意义：现实中的自然人；业主、居民、租户由关系产生，不是全局角色。
- 属性：`person_id`、`person_code`、`full_name`、`gender`、`phone`、`id_number`、`status`。
- 候选键：`person_id`；`person_code`；非空时 `id_number`。
- PK/FK：PK=`person_id`；无 FK。
- 规则：业务编码唯一；证件号可空且非空时唯一；生产环境证件号需加密/脱敏。
- FD：各候选键均决定全部属性。
- 范式：不保存 properties、roles 等集合字段，满足 3NF。
- 历史/删除：被业务历史引用后只停用。
- 查询：跨小区产权、当前居住、合同参与、付款、车辆。

### 4.7 Ownership（`ownership`，产权关系）

- 业务意义：谁在什么时间段拥有哪套房及其份额。
- 属性：`ownership_id`、`person_id`、`property_unit_id`、`ownership_share`、`start_date`、`end_date`、`certificate_no`。
- 候选键：`ownership_id`；业务候选键 `(person_id,property_unit_id,start_date)`。
- PK/FK：PK=`ownership_id`；FK 到 Person、PropertyUnit。
- 规则：业务候选键唯一；份额 `(0,1]`；结束日期不早于开始日期；end_date 可空表示当前有效。
- FD：`ownership_id → 其余属性`；`(person_id,property_unit_id,start_date) → ownership_share,end_date,certificate_no`。
- 范式：复合业务键的非键属性依赖完整键，不依赖 person_id 或 property_unit_id 的一部分；不保存人员名/房屋地址，满足 3NF。
- 历史/删除：用 end_date 结束关系，不覆盖/删除历史。
- 查询：产权清单、跨小区产权人、当前产权人。
- Service 规则：同一时段产权份额合计不超过 1、同一人同房产区间不重叠，需事务校验；单行 CHECK 无法可靠表达跨行聚合。

### 4.8 Residence（`residence`，实际居住关系）

- 业务意义：谁在什么时间段实际住在哪套房；与产权无推导关系。
- 属性：`residence_id`、`person_id`、`property_unit_id`、`residence_role`、`start_date`、`end_date`。
- 候选键：`residence_id`；`(person_id,property_unit_id,start_date)`。
- PK/FK：PK=`residence_id`；FK 到 Person、PropertyUnit。
- 规则：业务键唯一；日期区间合法；role 仅描述居住身份，不表示产权。
- FD：业务复合键决定 role/end_date；surrogate key 决定全部属性。
- 范式：不把 Ownership 属性带入，不保存地址快照，满足 3NF。
- 历史/删除：迁出时填 end_date。
- 查询：当前住户、无住户房产、小区入住率、某人实际住址。
- Service 规则：同一人主要住址的跨行唯一性、居住区间重叠策略由业务层管理。

### 4.9 Agency（`agency`，中介公司）

- 业务意义：接受业主具体房产授权的机构主体。
- 属性：`agency_id`、`agency_code`、`agency_name`、`license_no`、`contact_phone`、`address_line`、`status`。
- 候选键：ID、业务编码、证照编号。
- PK/FK：PK=`agency_id`；无 FK。
- 规则：编码与证照编号分别唯一。
- FD/范式：每个候选键决定其余属性；满足 3NF/BCNF。
- 历史/删除：停用，不删除已有授权/合同历史。
- 查询：按中介筛选房源、有效授权统计。

### 4.10 Authorization（`authorization`，中介授权）

- 业务意义：某业主把某套具体房产在某时段授权给某中介。
- 属性：`authorization_id`、`person_id`、`property_unit_id`、`agency_id`、起止日期、状态、`can_view/can_publish/can_edit_listing/can_manage_contract` 权限位。
- 候选键：ID；`(person_id,property_unit_id,agency_id,start_date)`。
- PK/FK：PK=`authorization_id`；三个 FK 分别到 Person、PropertyUnit、Agency。
- 规则：三方均非空；业务键唯一；时间区间合法；权限位为 0/1。
- FD：三元业务键决定授权时段结果、状态和权限；任一二元子集不能决定第三参与方。
- 范式：所有非键属性依赖完整三元业务键，不把 agency 信息或房产地址重复进来，满足 3NF。
- 历史/删除：撤销/过期改状态并保留日期，不删历史。
- 查询：某房产当前有效中介、某中介可发布房源、授权权限判断。
- DB/Service 边界：FK 保证三方存在；“授权人当时确为产权人”、授权区间与产权区间相容、同类授权是否重叠由 Service 事务校验。

### 4.11 RentalListing（`rental_listing`，出租房源）

- 业务意义：一套 PropertyUnit 的一次挂牌生命周期。
- 属性：`rental_listing_id`、`property_unit_id`、`listing_mode`、`publisher_person_id`、`authorization_id`、标题、描述、月租、押金、可入住日期、发布时间、关闭时间、状态。
- 候选键/主键：`rental_listing_id`。
- FK：PropertyUnit；SELF 模式的 Person；AGENCY 模式的 Authorization。
- 规则：SELF 必须有 publisher_person_id 且无 authorization_id；AGENCY 相反；复合 FK 强制授权对应同一 property_unit；金额和时间合法。
- FD：`rental_listing_id → 全部属性`。
- 范式：不把租金/挂牌状态放进 PropertyUnit；不复制小区、户型描述，满足 3NF。
- 历史/删除：关闭状态保留；同一房产未来可再次形成新 Listing。
- 查询：价格/户型/面积/中介/地域复合筛选，模糊检索，当前有效房源。

### 4.12 LeaseContract（`lease_contract`，租赁合同）

- 业务意义：房源成交后形成的合同；合同金额是签署时业务快照。
- 属性：`lease_contract_id`、`rental_listing_id`、`contract_no`、签署日、租期、月租快照、押金快照、状态。
- 候选键：ID、contract_no、rental_listing_id（唯一，实现 1:0..1）。
- PK/FK：PK=`lease_contract_id`；FK 到 RentalListing。
- 规则：一个 Listing 最多一个合同；租期结束不早于开始；签署日不晚于开始；金额合法。
- FD：每个候选键决定其余合同属性。
- 范式：参与人不作为 landlord_id/tenant_id 固定列；拆到 ContractParticipation，满足 3NF。
- 历史/删除：状态流转，合同不物理删除。
- 查询：房产合同历史、有效合同、合同参与人。

### 4.13 ContractParticipation（`contract_participation`，合同参与关系）

- 业务意义：一个人以出租方或承租方身份参与合同。
- 属性：`lease_contract_id`、`person_id`、`participant_role`、`is_primary`、`signed_at`。
- 候选键/主键：`(lease_contract_id,person_id,participant_role)`。
- FK：LeaseContract、Person。
- 规则：role 仅 LANDLORD/TENANT；is_primary 为 0/1。
- FD：完整复合键决定 `is_primary,signed_at`；这些属性不由合同或人员单独决定。
- 范式：无部分依赖和传递依赖，满足 3NF；自然支持多出租方、多承租方。
- 历史/删除：合同历史的一部分，RESTRICT 删除。
- 查询：合同各方、某人合同历史、多人签署状态。
- Service 规则：每份正式合同至少一名 LANDLORD 和一名 TENANT；主要联系人数量由事务校验。

### 4.14 FeeType（`fee_type`，收费项目）

- 业务意义：物业费、水费、电费等收费类别字典。
- 属性：`fee_type_id`、`fee_code`、`fee_name`、`billing_method`、`unit_name`、`status`。
- 候选键：ID、code、name；PK=ID。
- 规则：编码/名称唯一；计费方式 FIXED/AREA/METERED。
- FD/范式：候选键决定其余属性，满足 3NF。
- 历史/删除：已有账单引用后仅停用。
- 查询：收费项目列表、按计费方式检查标准。

### 4.15 FeeStandard（`fee_standard`，收费标准）

- 业务意义：某小区对某收费项目在某时段采用的单价。
- 属性：`fee_standard_id`、`community_id`、`fee_type_id`、`standard_name`、`unit_price`、起止日期、状态。
- 候选键：ID；`(community_id,fee_type_id,effective_from)`。
- PK/FK：PK=ID；FK 到 Community、FeeType。
- 规则：同小区同费种同生效日起点唯一；单价非负；日期合法。
- FD：业务复合键决定名称、单价、截止日、状态。
- 范式：非键属性依赖完整复合键；不重复小区/费种名称，满足 3NF。
- 历史/删除：新增版本，旧版本填 effective_to；不覆盖历史单价。
- 查询：账期有效标准、小区费率比较。
- Service 规则：同一小区同费种有效区间不得重叠，需跨行校验。

### 4.16 Meter（`meter`，计量表）

- 业务意义：安装在具体 PropertyUnit 上、对应一个计量收费项目的表具。
- 属性：`meter_id`、`property_unit_id`、`fee_type_id`、`meter_no`、安装/停用日期、初始读数、状态。
- 候选键：ID、meter_no；PK=ID。
- FK：PropertyUnit、FeeType。
- 规则：表号唯一；日期和初始读数合法。
- FD/范式：候选键决定其余属性；不保存房产地址或费种名，满足 3NF。
- 历史/删除：停表或拆表改状态，读数历史 RESTRICT。
- 查询：某房产表具、费种表具、当前有效表具。
- Service 规则：fee_type 必须为 METERED。

### 4.17 MeterReading（`meter_reading`，抄表记录）

- 业务意义：某计量表在某时刻的累计读数。
- 属性：`meter_reading_id`、`meter_id`、`reading_at`、`reading_value`、`reading_source`、`recorder_person_id`、`note`。
- 候选键：ID；`(meter_id,reading_at)`。
- PK/FK：PK=ID；FK 到 Meter；人工抄表人 Person 可空。
- 规则：同一表同一时刻唯一；读数非负；来源受 CHECK。
- FD：`(meter_id,reading_at) → reading_value,source,recorder,note`。
- 范式：读数属性依赖完整复合业务键，满足 3NF。
- 历史/删除：抄表是审计事实，不覆盖或删除。
- 查询：最新抄表（窗口函数）、区间用量、异常读数。
- Service 规则：累计读数通常不得下降、读数时间应在表具服役期内，需读取相邻行校验。

### 4.18 Bill（`bill`，物业账单）

- 业务意义：某房产某月的账单头。
- 属性：`bill_id`、`bill_no`、`property_unit_id`、`billing_year`、`billing_month`、`issued_at`、`due_date`、`status`。
- 候选键：ID、bill_no、`(property_unit_id,billing_year,billing_month)`。
- PK/FK：PK=ID；FK 到 PropertyUnit。
- 规则：同一房产同一月份仅一张账单；月份 1..12；状态受 CHECK。
- FD：任一候选键决定其余账单头属性。
- 范式：V1 不存可由 BillItem SUM 得到的 total_amount，避免受控冗余提前进入基线；满足 3NF。
- 历史/删除：账单不级联删除；通过状态取消。
- 查询：月度应收/实收、欠费账龄、某房产账单。

### 4.19 BillItem（`bill_item`，账单项目弱实体）

- 业务意义：账单内的一项具体计费结果，离开 Bill 没有独立业务身份。
- 属性：`bill_id`、`item_no`（账单内局部号）、`fee_standard_id`、前后读数 FK、`quantity`、`unit_price` 快照、生成列 `amount`、`note`。
- 候选键/主键：`(bill_id,item_no)`；没有脱离 bill_id 的全局候选键。
- FK：Bill、FeeStandard；前/当前 MeterReading 可空且成对出现。
- 规则：item_no 正数；数量和单价非负；计量读数必须同时为空或同时非空；amount 由数据库生成，避免金额不一致。
- FD：`(bill_id,item_no) → fee_standard_id,reading_ids,quantity,unit_price,note`；`quantity,unit_price → amount` 由生成列强制维护。
- 范式：非键属性依赖完整识别键；不存 fee_name；FeeType 通过 FeeStandard 推导，避免 `fee_standard_id → fee_type_id` 造成重复，满足 3NF。
- 历史/删除：与 Bill 一起成为财务历史，FK 为 RESTRICT 而非 CASCADE。
- 查询：账单明细、按费种汇总、计量收费因果追溯。
- Service 规则：收费标准所属小区必须与账单房产一致；两条读数必须属于同一 Meter，且该 Meter 属于账单房产和相应 METERED 费种。

### 4.20 Payment（`payment`，支付）

- 业务意义：某人对某账单的一次付款事实。
- 属性：`payment_id`、`payment_no`、`bill_id`、`payer_person_id`、`amount`、`paid_at`、`payment_method`、`external_transaction_no`、`status`。
- 候选键：ID、payment_no、非空时 external_transaction_no。
- PK/FK：PK=ID；FK 到 Bill、Person。
- 规则：金额大于 0；方法/状态受 CHECK；外部交易号非空时唯一。
- FD/范式：候选键决定其余属性；不把 Bill 总额或人员信息复制进来，满足 3NF。
- 历史/删除：支付事实不物理删除；失败/取消也保留。
- 查询：实收汇总、账单支付明细、付款人记录。
- Service 规则：成功支付合计不可超过应收、账单状态与支付状态需在事务中同步。

### 4.21 ParkingSpace（`parking_space`，车位）

- 业务意义：小区内的物理车位资产。
- 属性：`parking_space_id`、`community_id`、`space_no`、位置、类型、面积、物理状态。
- 候选键：ID；`(community_id,space_no)`。
- PK/FK：PK=ID；FK 到 Community。
- 规则：小区内编号唯一；面积正数；状态不保存可从 ParkingRight 推导的实时“占用”。
- FD/范式：业务复合键决定其余属性；满足 3NF。
- 历史/删除：维修/停用改状态。
- 查询：车位列表、有效权利与利用率。

### 4.22 Vehicle（`vehicle`，车辆）

- 业务意义：人员登记的车辆，不承担车位长期权利。
- 属性：`vehicle_id`、`person_id`、`plate_number`、类型、品牌、型号、颜色、状态。
- 候选键：ID、plate_number；PK=ID；FK 到 Person。
- 规则：车牌唯一；类型/状态受 CHECK。
- FD/范式：候选键决定其余属性；无 parking_space_id，满足 3NF。
- 历史/删除：换车时停用旧记录；车位权不受影响。
- 查询：某人车辆、车牌搜索。

### 4.23 ParkingRight（`parking_right`，车位权利）

- 业务意义：某人在某时间段拥有或租用某车位。
- 属性：`parking_right_id`、`person_id`、`parking_space_id`、`right_type`、起止日期、状态。
- 候选键：ID；`(person_id,parking_space_id,start_date)`。
- PK/FK：PK=ID；FK 到 Person、ParkingSpace；不引用 Vehicle。
- 规则：业务键唯一；日期合法；类型 OWNED/LEASED。
- FD：完整业务键决定类型、截止日和状态。
- 范式：非键属性依赖完整复合键，无车辆信息/人员信息冗余，满足 3NF。
- 历史/删除：结束权利填 end_date/status，保留历史。
- 查询：某人车位、当前车位权利、车位利用率。
- Service 规则：同一车位的排他性权利区间不得重叠，需事务校验。

### 4.24 FacilityType（`facility_type`，设施类型）

- 业务意义：床、沙发、空调、热水器等字典。
- 属性：`facility_type_id`、`facility_code`、`facility_name`、`category`、`default_mobility_mode`、`status`。
- 候选键：ID、code、name；PK=ID。
- 规则：编码/名称唯一；默认移动模式 FREE/FIXED/LIMITED。
- FD/范式：候选键决定其余属性，满足 3NF。
- 历史/删除：停用。
- 查询：设施选择器、按类别/移动方式筛选。

### 4.25 FacilityPlacement（`facility_placement`，设施二维放置）

- 业务意义：某个家具/设施在一套 PropertyUnit 完整户型图上的实例与位置。
- 属性：`facility_placement_id`、`property_unit_id`、`facility_type_id`、标签、`x_ratio/y_ratio/width_ratio/height_ratio`、旋转、`mobility_mode`、锚点、移动半径、安装备注。
- 候选键/主键：`facility_placement_id`。
- FK：PropertyUnit、FacilityType。
- NULL/规则：位置与尺寸均为 0..1 比例且不能越界；FREE 不设锚点；FIXED 必须有锚点且半径为 0；LIMITED 必须有锚点且半径大于 0。
- FD：placement_id 决定全部实例属性。
- 范式：设施类型固有信息留在 FacilityType；实例坐标只依赖 Placement；满足 3NF。
- 历史/删除：配置可由应用显式删除/版本化；本 V1 保留审计时间。
- 查询：加载某套房完整覆盖层、统计设施配置、检查受限设施。
- 空调设计：`anchor_*` 表示墙孔/安装中心，`movement_radius_ratio` 表示有限移动范围，`installation_note` 记录管线、排水等约束。

## 5. ER → Relation Mapping 重点

1. M:N `Person ↔ PropertyUnit` 不能放在 Person 或 PropertyUnit 的单值列中，分别映射为 Ownership 和 Residence；二者语义、函数依赖和时间线均不同。
2. 三元 Authorization 的任一二元子集都不能完整表示“哪位业主的哪套房授权哪家机构”，因此三个参与实体标识必须在同一独立关系内。
3. `Person ↔ LeaseContract` 的 M:N 关系映射为 ContractParticipation，并把 LANDLORD/TENANT 作为参与关系属性，消除固定单房东/单租户限制。
4. BillItem 对 Bill 存在识别依赖，使用 `(bill_id,item_no)`，体现弱实体；不创建脱离账单的全局 item_id。
5. `Person ↔ ParkingSpace` 的时态 M:N 关系映射为 ParkingRight；Vehicle 与 Person 单独 1:N，换车不影响车位权。
6. 1:N 层级使用 N 端 FK：Building.community_id、BuildingUnit.building_id、PropertyUnit.building_unit_id。

## 6. NULL 与历史原则

- NULL 只表示业务上确实可缺省：结束日期仍有效、模板尚未分配、媒体尚未上传、系统抄表无人工记录人等。
- 核心归属 FK、业务编码、金额与状态均不允许 NULL。
- Ownership、Residence、Authorization、FeeStandard、ParkingRight 使用起止区间保留历史。
- PropertyUnit、Community、Agency、基础字典优先状态停用。
- Bill、BillItem、Payment、LeaseContract 不允许级联物理删除。

## 7. V1 索引边界

已经建立：PK、所有 UNIQUE、FK/关联方向的必要单列索引。没有提前建立：

- 账单 `(billing_year,billing_month,status)` 高级实验索引；
- 房源多条件复合/覆盖索引；
- LIKE 全文索引；
- POINT/SPATIAL INDEX；
- 汇总/物化表。

后续 Stage 13 使用真实数据和 `EXPLAIN / EXPLAIN ANALYZE` 比较无索引、单列、复合、覆盖、空间索引及选择/投影下推效果。
