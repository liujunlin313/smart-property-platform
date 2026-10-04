# V10 最终数据库 ER 模型审计

> 审计对象：`C:\Projects\数据库课程设计\多小区智慧物业管理系统_MERGED_V1`  
> 审计方式：仅执行 `SELECT`、`SHOW`、`information_schema` 查询，并交叉阅读 V1–V10 Migration、Mapper XML、Java Service/Model。  
> 事实基线：当前 `community_db`，Flyway V1–V10 均成功，47 张基础表、2 个视图、77 个外键、50 个唯一约束、108 个 CHECK。  
> 本文不修改数据库，也不把工程支撑表伪装成物业现实实体。

---

## 1. 模型分层结论

| 层级 | 数量 | 用于课堂的讲法 |
|---|---:|---|
| 第一层：核心业务模型 | 20 | 不存在这些实体/关系，就不能准确描述“多小区物业管理”的现实问题。它们进入全局核心 ER。 |
| 第二层：真实业务扩展 | 13 | 现实中存在且系统已建模，但不是第一次汇报必须展开的核心矛盾。通常进入相应子 ER 或作为外围扩展。 |
| 第三层：工程支撑模型 | 14 | 认证、权限、范围、文件、审计、演进等让系统安全可运行的模型；不混入全局业务 ER 主体。 |

### 全局核心 ER 最终实体数

**20 个第一层实体**进入 `01_global_core_er.html`。全局图仅显示 PK 和 1–3 个关键业务字段；详细属性由 5 张子 ER 图承担。

---

## 2. 全部 47 张基础表分类

| 表 | 分类 | 现实含义 | 全局 ER | 子 ER | 分类理由 |
|---|---|---|---|---|---|
| `community` | 第一层 | 小区 | 是 | 空间资产、居民服务 | 物业管理的最高空间单元。 |
| `building` | 第一层 | 楼栋 | 是 | 空间资产、居民服务 | 小区下的实际管理楼栋。 |
| `building_unit` | 第一层 | 单元 | 是 | 空间资产 | 楼栋下的管理粒度，房产归属此处。 |
| `property_unit` | 第一层 | 房产/房屋 | 是 | 全部业务子图 | 系统最小完整房产单位，是产权、居住、收费、表计、车证的共同锚点。 |
| `layout_template` | 第一层 | 户型模板 | 是 | 空间资产 | 可复用房屋户型定义，不是每套房的复制数据。 |
| `person` | 第一层 | 自然人 | 是 | 人员产权居住、车证、通知 | 业主、住户、缴费人、车辆持有人、通知收件人均指向此实体。 |
| `ownership` | 第一层 | 产权关系 | 是 | 人员产权居住 | 人与房的时间化产权关系，支持共有与历史。 |
| `residence` | 第一层 | 居住关系 | 是 | 人员产权居住、车证 | 人与房的时间化居住关系，独立于产权。 |
| `fee_type` | 第一层 | 收费类别 | 是 | 收费 | 例如水、电、物业等的类别定义。 |
| `fee_standard` | 第一层 | 收费标准 | 是 | 收费 | 在特定小区/生效期的具体计价规则。 |
| `meter` | 第一层 | 表计设备 | 是 | 收费 | 稳定设备实体，关联房产与计费类别。 |
| `meter_reading` | 第一层 | 抄表读数 | 是 | 收费 | 某一时刻的表计事实，形成时间序列。 |
| `bill` | 第一层 | 应收账单 | 是 | 收费 | 按房产和账期形成的应收单据。 |
| `bill_item` | 第一层 | 账单费用明细 | 是 | 收费 | 一张账单由多项费用构成，可引用前后读数。 |
| `payment` | 第一层 | 实际缴费 | 是 | 收费 | 对账单的实际支付事实，不与应收账单混合。 |
| `vehicle` | 第一层 | 车辆 | 是 | 车辆车证 | 人拥有/登记的车辆。 |
| `parking_permit_application` | 第一层 | 车证申请 | 是 | 车辆车证 | 居民针对房产与车辆提出的待审事件。 |
| `parking_permit` | 第一层 | 车证 | 是 | 车辆车证 | 审核成功后形成的有效凭证。 |
| `notification` | 第一层 | 物业通知 | 是 | 居民服务通知 | 物业发布的业务信息实体。 |
| `notification_recipient` | 第一层 | 通知收件关系 | 是 | 居民服务通知 | 一条通知展开为多个实际 Person 接收人，记录 TO/CC/已读。 |
| `agency` | 第二层 | 中介机构 | 外围 | 不单列 | 租赁代理业务中的真实组织实体。 |
| `agency_staff` | 第二层 | 中介工作人员 | 外围 | 不单列 | 人员在中介机构中的任职关系。 |
| `authorization` | 第二层 | 房产委托/授权 | 外围 | 不单列 | 产权人、房产和中介间的真实授权事实。 |
| `rental_listing` | 第二层 | 租赁挂牌 | 外围 | 不单列 | 房产对外出租的挂牌生命周期。 |
| `lease_contract` | 第二层 | 租赁合同 | 否 | 不单列 | 真实扩展模型；当前数据为 0，不作为主讲数据流。 |
| `contract_participation` | 第二层 | 合同参与关系 | 否 | 不单列 | 合同与人员的多对多参与关系，当前数据为 0。 |
| `parking_space` | 第二层 | 物理停车位 | 否 | 不单列 | 真实扩展，但当前车证主线不以固定车位为核心，数据为 0。 |
| `parking_right` | 第二层 | 停车位权益 | 否 | 不单列 | 人与车位的时间化权益，当前数据为 0。 |
| `residence_parking_eligibility` | 第二层 | 租住停车申请资格 | 外围 | 车辆车证 | 以 Residence 为主键的一对零或一资格记录，表达租户是否能申请。 |
| `resident_incident` | 第二层 | 居民异常/报事 | 外围 | 居民服务通知 | 住户向物业提交的事项，当前有少量样例数据。 |
| `facility_type` | 第二层 | 户型设施类型 | 否 | 不单列 | 户型图/设施扩展定义。 |
| `facility_placement` | 第二层 | 设施布置 | 否 | 不单列 | 设施在具体房产平面图上的位置，当前数据为 0。 |
| `spatial_boundary` | 第二层 | 空间边界 | 否 | 不单列 | 小区/楼栋真实 GeoJSON 边界，当前数据为 0。 |
| `app_account` | 第三层 | 系统登录账户 | 否 | 不进入业务 ER | 软件身份，不等同于 Person；用于认证、锁定、令牌版本。 |
| `app_role` | 第三层 | 系统角色 | 否 | 不进入业务 ER | 权限控制的工程概念。 |
| `app_permission` | 第三层 | 系统权限 | 否 | 不进入业务 ER | 接口操作授权的工程概念。 |
| `role_permission` | 第三层 | 角色权限关系 | 否 | 不进入业务 ER | RBAC 桥接表。 |
| `account_global_role` | 第三层 | 全局角色分配 | 否 | 不进入业务 ER | 账户与全局角色的工程关系。 |
| `account_community_scope` | 第三层 | 小区数据范围 | 否 | 不进入业务 ER | 限制账户可以管理哪些小区。 |
| `account_agency_role` | 第三层 | 账户中介角色 | 否 | 不进入业务 ER | 中介端身份授权，当前数据为 0。 |
| `refresh_token_session` | 第三层 | 刷新令牌会话 | 否 | 不进入业务 ER | JWT 刷新、撤销、设备会话支撑。 |
| `file_object` | 第三层 | 对象存储元数据 | 否 | 仅轻量说明 | MinIO 中二进制对象的元信息，不是物业实体。 |
| `layout_template_floor_plan` | 第三层 | 模板默认户型图关联 | 否 | 空间图轻量说明 | 工程文件关联，模板仍属于核心业务。 |
| `layout_template_floor_plan_variant` | 第三层 | 面积变体户型图关联 | 否 | 空间图轻量说明 | 工程文件关联，解决同户型不同面积图的展示。 |
| `property_floor_plan` | 第三层 | 单房覆盖图关联 | 否 | 空间图轻量说明 | 工程文件关联；当前数据为 0。 |
| `asset_change_audit` | 第三层 | 资产变更审计 | 否 | 不进入业务 ER | 记录系统操作前后数据，属于治理/审计支撑。 |
| `flyway_schema_history` | 第三层 | 数据库版本历史 | 否 | 不进入业务 ER | 框架演进元数据，不是物业业务事实。 |

---

## 3. 第一层实体及关系核验

### 3.1 空间资产

| 实体 | PK | 主要 FK | 关键字段 | 已核实关系 |
|---|---|---|---|---|
| Community / 小区 | `community_id` | — | `community_code`、`community_name`、地址、坐标、状态 | 1 个小区包含 0..N 栋楼。 |
| Building / 楼栋 | `building_id` | `community_id → community` | `building_no`、楼层数、坐标、状态 | `UQ(community_id, building_no)`；每栋楼属于 1 个小区，包含 0..N 单元。 |
| BuildingUnit / 单元 | `building_unit_id` | `building_id → building` | `unit_no`、`unit_name`、状态 | `UQ(building_id, unit_no)`；每单元属于 1 栋楼，包含 0..N 房产。 |
| PropertyUnit / 房产 | `property_unit_id` | `building_unit_id → building_unit`；`layout_template_id → layout_template` | `property_no`、面积、楼层、`occupancy_status` | `UQ(building_unit_id, property_no)`；是多个核心关系的锚点。 |
| LayoutTemplate / 户型模板 | `layout_template_id` | — | `layout_code`、`layout_name`、`layout_type`、标准面积 | 1 个模板可复用于 0..N 房产。 |

### 3.2 人员、产权与居住

| 实体 | PK | 主要 FK | 关键字段 | 已核实关系 |
|---|---|---|---|---|
| Person / 人员 | `person_id` | — | `person_code`、姓名、证件、电话、状态 | 可出现在产权、居住、车辆、通知收件、缴费、抄表等关系中。 |
| Ownership / 产权关系 | `ownership_id` | `person_id → person`；`property_unit_id → property_unit` | `ownership_share`、`start_date`、`end_date`、产权证号 | 连接 Person 与 PropertyUnit；`UQ(person_id, property_unit_id, start_date)` 表明为**时间化 N:M**，而非静态单向从属。 |
| Residence / 居住关系 | `residence_id` | `person_id → person`；`property_unit_id → property_unit` | `residence_role`、`relation_to_responsible`、起止/预计结束日期 | 同样为时间化 N:M。`UQ(person_id, property_unit_id, start_date)` 保存历史；额外当前态唯一列保证当前人在同一时间只有一处居住、同房只有一个责任人。 |

**关键业务结论：Ownership ≠ Residence。**产权可以由房东持有，居住可以由租客或家庭成员承担；两者都存在时间维度，不能合并为“房主=当前居民”。

### 3.3 收费与计量

| 实体 | PK | 主要 FK | 关键字段 | 已核实关系 |
|---|---|---|---|---|
| FeeType / 收费类别 | `fee_type_id` | — | `fee_code`、名称、`billing_method`、单位 | 1 类别对应 0..N 个收费标准和 0..N 表计。 |
| FeeStandard / 收费标准 | `fee_standard_id` | `community_id → community`；`fee_type_id → fee_type` | 单价、生效区间、状态 | 具体小区、具体时间的收费规则；不是费用类别本身。 |
| Meter / 表计 | `meter_id` | `property_unit_id → property_unit`；`fee_type_id → fee_type` | `meter_no`、安装/停用、初始读数 | 稳定设备；1 表计产生 0..N 条读数。 |
| MeterReading / 抄表读数 | `meter_reading_id` | `meter_id → meter`；`recorder_person_id → person` | `reading_at`、`reading_value`、来源 | `UQ(meter_id, reading_at)`；是时间序列事实。 |
| Bill / 账单 | `bill_id` | `property_unit_id → property_unit` | `bill_no`、账期、到期日、状态 | `UQ(property_unit_id,billing_year,billing_month)`；1 账单有 1..N 明细和 0..N 支付。 |
| BillItem / 账单明细 | `(bill_id,item_no)` | `bill_id → bill`；`fee_standard_id → fee_standard`；前/后读数 FK | 数量、单价、金额 | 将费用组成从 Bill 分离，复合 PK 保证每张账单内明细编号唯一。 |
| Payment / 缴费 | `payment_id` | `bill_id → bill`；`payer_person_id → person` | 支付号、金额、方式、状态、支付时间 | 多次支付可对应同一账单；成功支付参与实收统计。 |

### 3.4 车辆与车证

| 实体 | PK | 主要 FK | 关键字段 | 已核实关系 |
|---|---|---|---|---|
| Vehicle / 车辆 | `vehicle_id` | `person_id → person` | 车牌、车辆类型、品牌、状态 | 1 人可登记 0..N 车辆；`plate_number` 唯一。 |
| ParkingPermitApplication / 车证申请 | `parking_permit_application_id` | 人员、车辆、房产、可选 Residence、审核账户 | 申请号、资格来源、报价、申请状态 | 1 车辆/人员可有 0..N 申请；一个申请审核后最多产生 0..1 车证。 |
| ParkingPermit / 车证 | `parking_permit_id` | `parking_permit_application_id → application`（唯一）；人员、车辆、房产、Residence | 车证号、有效期、状态、费用 | `UQ(parking_permit_application_id)` 确认申请到车证为 0..1；不是固定车位分配。 |

`residence_parking_eligibility` 是 `residence_id` 主键/FK 的 0..1 扩展记录，表达租住关系是否具有申请资格；其 `eligible`、原因和备注不是停车位实体。

### 3.5 居民服务通知

| 实体 | PK | 主要 FK | 关键字段 | 已核实关系 |
|---|---|---|---|---|
| Notification / 通知 | `notification_id` | `sender_account_id → app_account` | 分类、状态、主题、正文、发送时间 | 1 条通知可产生 1..N 条收件关系。 |
| NotificationRecipient / 通知收件 | `notification_recipient_id` | `notification_id → notification`；`person_id → person` | `recipient_role`、投递状态、`read_at`、个性化 payload | `UQ(notification_id, person_id)`；因此 Notification 与 Person 是由本表实现的 N:M，TO/CC 为关系属性。 |

通知发送时可从 Community/Building/PropertyUnit/Person 选择范围，但源代码由 `NotificationServiceImpl` 在服务层解析为具体 `person_id` 后写入 `notification_recipient`。空间范围**不是 Notification 表上的物理 FK**，不得误画成持久直接外键。

---

## 4. 关系类型与课堂讲解重点

| 关系 | 基数 | 依据 | 课堂讲法 |
|---|---|---|---|
| Community—Building | 1:N | `building.community_id` FK，且 FK 非唯一 | 一个小区有多栋楼。 |
| Building—BuildingUnit | 1:N | `building_unit.building_id` FK，且 FK 非唯一 | 楼栋下的单元是独立管理粒度。 |
| BuildingUnit—PropertyUnit | 1:N | `property_unit.building_unit_id` FK，且 FK 非唯一 | 房产归属单元而非直接跳到小区。 |
| LayoutTemplate—PropertyUnit | 1:N | `property_unit.layout_template_id` FK，且 FK 非唯一 | 模板可复用，避免每套房复制户型定义。 |
| Person—Ownership—PropertyUnit | N:M（时间化） | 两端 FK + `UQ(person_id,property_unit_id,start_date)` | 共有产权与产权历史。 |
| Person—Residence—PropertyUnit | N:M（时间化） | 两端 FK + 起止日期 + 当前态唯一列 | “拥有”与“居住”是两个现实关系。 |
| FeeType—FeeStandard | 1:N | `fee_standard.fee_type_id` FK | 类别定义与具体收费规则分离。 |
| PropertyUnit—Meter—MeterReading | 1:N:N | `meter.property_unit_id`、`meter_reading.meter_id` FKs | 设备稳定、读数持续发生。 |
| PropertyUnit—Bill—BillItem | 1:N:1..N | `bill.property_unit_id`；`bill_item.bill_id` + 复合 PK | 月度应收与费用组成独立。 |
| Bill—Payment | 1:N | `payment.bill_id` FK，非唯一 | 实际缴费不等同于应收账单。 |
| Person—Vehicle | 1:N | `vehicle.person_id` FK | 人可持有多个车辆。 |
| Application—Permit | 1:0..1 | Permit 的 application FK 有唯一约束 | 一次申请最多核发一张车证。 |
| Notification—Recipient—Person | N:M | 两端 FK + `UQ(notification_id,person_id)` | 发送范围最终展开为实际收件人。 |
| Residence—ParkingEligibility | 1:0..1 | `residence_id` 同时为 PK 与 FK | 租住资格是居住关系的可选业务扩展。 |

没有任何需要“人工猜测”的核心关系；本报告中的基数均能由 FK、唯一约束、复合键、CHECK 或服务逻辑交叉确认。未进核心图的合同、停车位、空间边界等关系，因当前样例数据为 0 或不属主讲核心，而不是因为结构无法确认。

---

## 5. 六张 ER 图内容边界

| 图 | 文件 | 展示内容 | 刻意不放入的内容 |
|---:|---|---|---|
| 01 | `01_global_core_er.html` | 五个核心域的 20 实体与主关系 | 账户、权限、Token、文件元数据等工程支撑表。 |
| 02 | `02_spatial_asset_er.html` | 小区—楼栋—单元—房产—户型模板 | 不引入虚构 Room；文件关联仅作一句说明。 |
| 03 | `03_person_ownership_residence_er.html` | Person、Ownership、Residence、PropertyUnit，突出时间关系 | 不将账户混入人员现实模型。 |
| 04 | `04_billing_er.html` | 收费类别/标准、表计/读数、账单/明细/支付 | 不把视图当作物理业务实体。 |
| 05 | `05_vehicle_parking_permit_er.html` | 人、车辆、居住资格、申请、车证、房产 | ParkingSpace/ParkingRight 不走主线。 |
| 06 | `06_resident_service_notification_er.html` | 通知、收件人、人员及居民异常扩展；标明空间范围由服务解析 | 不把空间选择范围误画成通知物理 FK。 |

---

## 6. 工程支撑模型应如何讲

工程支撑表不进入业务 ER 主体，但课堂上可用一页或 30 秒说明：

> 物业现实世界没有 JWT Token、角色表或对象存储键；但多个居民、物业人员和管理员共同使用系统时，必须解决登录身份、可操作权限、可查看小区范围、刷新会话，以及户型图对象的可靠保存。因此这些表属于让业务模型能够安全稳定运行的软件工程支撑层。

当前可举出的真实支撑证据：

- `app_account`、角色/权限桥接表和 `account_community_scope` 支撑 JWT、权限与小区数据范围。
- `refresh_token_session` 保存刷新令牌哈希、过期和撤销状态。
- `file_object` 与三类户型图关联表将 MySQL 元数据同 MinIO 对象存储分离。
- `asset_change_audit` 记录资产修改前后 JSON 与操作者。
- `flyway_schema_history` 记录 V1–V10 演进。

---

## 7. 制图验证清单

六张 `docs/ppt-er/*.html` 均遵循：

- 离线单文件 HTML，嵌入 SVG/CSS，不依赖 CDN、网络或运行中的项目服务；
- 固定 1920×1080 的 16:9 画布，四周保留安全边距；
- 中文使用系统字体，实体标题 19px 以上，字段 14px 以上；
- 连线位于实体框下层，从框边缘连接；关系标签使用带底色的标签；
- 全局图减少字段而不删除核心实体；子图再展开关键属性；
- 以统一的 `1`、`0..1`、`N` 基数标记，而非混用多种符号系统。

本轮对六图完成了 1920×1080 画布、实体框、文本锚点、连线路由和 SVG 标签层级的静态坐标审查，重点为重叠、文字溢出、连线穿框、无效交叉、标签碰撞和边缘裁切；其中已修正全局图的跨域穿框风险和收费图的重复实体/错误关系风险。当前执行环境禁止浏览器直接访问本机 `file://` 图稿，故没有把“浏览器截图渲染”写成已完成事实。交付图均为离线 HTML，可直接在本机 Edge 打开后以 1920×1080 进行最终人工投影预览。
