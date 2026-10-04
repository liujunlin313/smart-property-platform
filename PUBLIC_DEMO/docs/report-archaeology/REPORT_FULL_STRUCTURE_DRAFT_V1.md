# 《多小区智慧物业管理系统》数据库课程设计报告完整结构设计稿 V1

> 本稿是“三级目录＋半正文框架”，不是提交版正文。建议最终正文约 3.6 万字。工程证据基准为 C:\Projects\数据库课程设计\多小区智慧物业管理系统_PUBLIC_DEMO（下称“项目根目录”）；数据库最终结构证据为同级 C:\Projects\数据库课程设计\deployment_backups\community_db_before_public_demo_20260901.sql（下称“2026-09-01 备份”）。本轮未查询在线库、未运行测试；凡涉及当前数据规模、性能和运行状态，均需注明历史日期，正式写作前再复核。前两轮考古索引见 docs/report-archaeology/PROJECT_REPORT_FACT_BASELINE.md、DATABASE_DESIGN_ARCHAEOLOGY.md、REPORT_EVIDENCE_MAP.md、REPORT_SECOND_PASS_HIGHLIGHT_AUDIT.md。

> 路径约定：未给绝对路径的证据文件均相对于项目根目录；V1～V10 指 database/migrations/ 中对应 SQL；stage1/、stageb/、asset/、access/、parking/ 等 XML 缩写指 backend/src/main/resources/mapper/ 下的子目录；Java 类名缩写指 backend/src/main/java/com/smartproperty/platform/ 下按包名定位。这个约定只为减少三级提纲中的重复长路径，正式正文首次引用文件时应给完整相对路径和必要的 XML id。

## 写作定位、章节接续和篇幅

本报告沿用课程设计常见的“系统定义和需求→ER→关系模式与范式→物理结构→程序访问→测试运行→总结”路径，但不将项目写成 Spring Boot 技术栈清单。先从现实物业规则提取实体和联系，再说明这些联系怎样转为键、外键、约束和查询；程序与页面仅作为数据库设计被真实使用的验证界面。重要结论按“源码/SQL 直接证明、历史日志或阶段文档证明、合理推断、尚缺证据”区分，不能把设计意图写成已实现行为。

| 章 | 暂定正文篇幅 | 章节任务和与下一章的衔接 |
|---|---:|---|
| 1 项目概述与开发环境 | 2,200 字 | 界定物业多域与系统边界，引出需求分析。 |
| 2 需求分析与系统边界 | 3,500 字 | 把参与者、数据、事务、查询和质量需求写成可验证的问题，引出 ER。 |
| 3 数据库概念结构设计 | 4,500 字 | 从现实关系构建主 ER 与子 ER，引出关系模式。 |
| 4 逻辑结构与范式 | 5,000 字 | 说明键、函数依赖、拆表与有界物理折中，引出约束/索引/View。 |
| 5 完整性、索引、视图与物理设计 | 6,000 字 | 分析数据库自身如何防错、提供访问路径和统一统计口径。 |
| 6 多小区权限与数据范围 | 3,000 字 | 将“多小区”落实为关系表、对象校验和 SQL 行过滤。 |
| 7 关键业务与 MyBatis 访问 | 4,000 字 | 用真实页面→接口→SQL 链验证前述设计。 |
| 8 数据库演进与数据生命周期 | 3,200 字 | 从静态结构转入历次变化、备份与恢复。 |
| 9 测试、验证与性能 | 3,500 字 | 用负例、事务、权限、执行计划和恢复结果检验设计。 |
| 10 总结与反思 | 1,100 字 | 归纳课程知识的应用并诚实陈述未解决边界。 |
| **合计** | **36,000 字** | 不以附录代码和截图填充字数；按学校模板再调整。 |

图表统一编号为“章-序号”。本稿每节列出优先证据与建议插图；截图只截能证明该节命题的局部，并标明当时环境和日期。完整 46 张主要表（另有 Flyway 历史表）的字段字典不宜铺满正文：正文精选 8～12 张关键表，其余入附录。上一轮提到的“完整 25 表字典”只对应 V1 初版，不代表最终数据库总表数。

# 第一章 项目概述与开发环境

本章的论点不是“选用了哪些框架”，而是“为什么这个现实场景需要关系数据库，并且需要让数据长期可靠地工作”。写作从业务对象和状态变化切入，技术环境放在问题之后。章末以业务多样性和数据访问边界导向第二章需求。

## 1.1 选题背景与课程设计价值

物业管理同时处理相对稳定的空间资产、会随时间改变的人房关系、周期账务、实收记录和跨社区授权；这些事实并非一张可更新的电子表格。数据库课程所讨论的实体/联系、函数依赖、约束、事务和查询优化，在这里都有可以观察的业务后果。开篇可用“一套房从建立、入住、出账到缴费”的例子说明它会触及多张关系表。

### 1.1.1 从单小区 CRUD 到多社区业务域

系统以 Community 为顶层边界，其下有 Building、BuildingUnit、PropertyUnit；人员、产权、居住、挂牌、账单和表计最终与具体房产或社区关联。4 个社区、14 栋楼、40 个单元、528 套房是 2026-09-01 备份及历史核查的演示规模，不能仅凭“多插入几行社区”说明多小区：不同员工可见哪些社区还需在第六章证明。这里先提出“共库多社区＋可变化的授权范围”这一核心设计问题。

证据与图：database/migrations/V1__init_schema.sql 的空间层级 DDL，docs/PPT_FINAL_STATE_AUDIT.md 的历史计数；建议放“社区→楼→单元→房产”四层示意，不先贴权限实现。

### 1.1.2 房产作为长期业务事实的锚点

PropertyUnit 是最小可管理房产单位，不另建 Room 并按房间出租；产权、居住、账单、表计、挂牌围绕整套房产形成不同事实。人物身份与房产状态不能互相替代：一个人可以曾住多处，房产也会换住户，但旧记录不能被新值覆盖。本节明确研究对象是“业务事实及其关联”，而不是页面数量。

证据与图：database/tests/05_seed_assertions.sql 的 Room/Occupancy 断言、V1 中 property_unit/ownership/residence/bill；可放房产为中心的轻量主题图。

## 1.2 平台职责与数据流

技术介绍只说明一条数据如何从页面进入服务再进入 MySQL，以及非结构化文件为何不放到关系表。不要花篇幅介绍框架官网特性；每个组件必须回答它对数据组织、控制或验证的具体作用。

### 1.2.1 关系数据库、业务服务和显式 SQL

MySQL 8 保存实体、关联、约束和两个财务 View；Spring Boot 处理身份、事务、规则和 HTTP；MyBatis Mapper 接口/XML 保留可审计的 JOIN、动态条件和聚合 SQL；Vue、Element Plus 与 ECharts 展现结果。请求链可概括为页面→API→Controller→Service→Mapper XML→MySQL，再从 VO 回到页面。第七章会用真实接口展开，而非在本章罗列所有类。

证据与图：backend/pom.xml、frontend/package.json、backend/src/main/resources/mapper/、frontend/src/api/http.js；建议放仅含职责和数据流向的架构图。

### 1.2.2 文件、部署与数据库版本的分工

户型图等二进制对象置于 MinIO，MySQL 的 file_object 记录 bucket、object key、类型、校验值和业务关系；Docker Compose 将 MySQL、Redis、MinIO、后端、Web 组合成可迁移演示环境，named volume 负责数据生命周期；Flyway V1–V10 记录大部分结构演进。这里应先承认边界：Redis 可用性不等于已经证明业务缓存加速；MySQL 与 MinIO 不是同一原子事务；最终结构并非已证明能由现存迁移单独重建。

证据与图：compose.yaml、V3/V9/V10、backend/src/main/java/com/smartproperty/platform/file/service/FloorPlanService.java、docs/DOCKER_STAGE1_RUNBOOK.md；建议放“MySQL 元数据↔MinIO 对象”简图，具体事务留第七、八章。

## 1.3 研究方法与证据版本

报告不是宣传材料，必须交代“源码中的规则”“某日备份中的结构”和“历史测试时的结果”分别是什么。V1 初始 25 表、最终 46 张主要表、Flyway V1–V10 和 2026-09-01 备份并非一个时间点；V1 测试隔离库中的样例数据也不能冒充最终公开演示库。这个证据原则会贯穿全文。

### 1.3.1 事实基线、时间戳和非目标

本稿将最终 Schema 以 2026-09-01 备份为准，运行效果引用已有 run 日志或阶段报告并标时间；正式提交前若系统变化，应重新导出可公开的结构和指标。本报告重心是数据库，不把地图底图、CSS、Tunnel 或普通容器启动当成数据库创新；也不宣称已实现完整线上支付写链、正式合同闭环或生产级负载能力。第二章据此限定需求讨论的“已经落地”与“模型预留”。

证据与图：docs/report-archaeology/PROJECT_REPORT_FACT_BASELINE.md、docs/PPT_FINAL_STATE_AUDIT.md；建议放三列“设计文件／最终备份／历史运行证据”的证据口径表。

# 第二章 需求分析与系统边界

本章把数据库设计还原到需求而不是从表名倒推业务。讨论参与者、长期数据、写入事务、组合查询和质量要求；每项都为后面某张关系表、某条约束、某个索引或测试提供来由。章末形成概念建模输入。

## 2.1 参与者与权限需求

系统至少有系统管理员、物业工作人员与绑定 Person 的居民账号。系统管理员执行全局管理；物业员工在被授予的小区范围内处理工作；居民只访问自身人房关系、账单、通知和报事。前端菜单是使用体验，真正的权限必须由后端和 SQL 约束。

### 2.1.1 功能许可与对象范围是两个不同问题

PROPERTY_VIEW、BILL_VIEW 等权限回答“能否做这一类操作”，account_community_scope 回答“能对哪些社区的数据做”。同是物业工作人员，一人管一个社区、另一人管两个社区，功能权限可以相同而查询结果不同。后续第六章将分别核对角色权限、Scope 获取、列表过滤和单对象访问；此处只定义验收命题。

证据与图：database/migrations/V4__stage_f_access_scope_boundary_notification.sql、docs/STAGE_H_R1_IDENTITY_SCOPE_SECURITY_REPAIR_REPORT.md；可放“角色×操作”和“账号×社区”两张小矩阵，避免合成一张难读大表。

### 2.1.2 居民与中介的业务边界

Person 不等同于 app_account：人可以存在而未有登录账号；居民门户依据账号绑定的人访问个人事实。中介机构、员工、授权与挂牌在模型中独立，但最终备份中部分中介/合同周边结构的创建来源缺失，且合同等最终数据为空。因此需求可阐明中介授权与出租场景，不能把“建有合同表”直接写成现已完成在线签约。

证据与图：V1 person/agency/authorization/rental_listing、2026-09-01 备份、backend/src/main/java/com/smartproperty/platform/residentportal/；建议一张参与者与数据边界图。

## 2.2 核心数据需求及生命周期

应按“要长期保存什么事实”组织需求：稳定主数据、带时效的关系、发生过的交易/事件、可重复计算的统计结果来源。不能仅列页面“有房产管理、有账单管理”；页面未必体现历史和约束。

### 2.2.1 空间层级、人员与时态人房关系

社区、楼栋、单元、房产各有独立标识与父子关系。Person 记录现实人员；Ownership 与 Residence 分别记录产权和居住的起止日期、份额/角色。若在 PropertyUnit 直接存 owner_id 和 resident_id，搬迁或转让会覆盖历史，也难表达多人关系。房产当前占用状态只是快照，不能代替 Residence 事实表。

证据与图：V1 的 community/building/building_unit/property_unit/person/ownership/residence；docs/database/NORMALIZATION_REPORT_V1.md；建议画“主实体＋两种带时间的联系”概念图。

### 2.2.2 收费、计量和资金事实

费用类型定义“收什么”，费用标准定义社区与时间范围中的价格规则，表计读数记录用量来源，Bill 是某房某账期的应收凭据，BillItem 是不同费用项目的构成，Payment 是后来发生的缴费。单一“收费金额”字段无法支持一张账单多项目、多次支付及未支付账单的准确统计。需求中还应写清：项目级欠费分摊并未由现有 Payment 数据直接给出，不能无依据地推算。

证据与图：V1 的 fee_type/fee_standard/meter/meter_reading/bill/bill_item/payment，notification/charge/ChargeNoticeComposer.java；建议“费标→账单项→账单←付款”数据流图。

### 2.2.3 租赁、停车、通知与文件的各自事实

Authorization 是人对特定房产向特定机构的授权，RentalListing 是挂牌行为；车位使用权、车辆、车证资格、申请与发证各有不同生命周期；Notification 与 Person 是多对多，投递/已读信息属于 NotificationRecipient；户型图文件需要对象存储和数据库关系元数据。需求分析应先讲为何不能合并，再在概念模型与关系模式中逐一落实。

证据与图：V1/V4/V8/V9/V10；docs/ppt-er/05_vehicle_parking_permit_er.html、06_resident_service_notification_er.html；只挑两张最能说明关系属性的子图。

## 2.3 写入事务与查询需求

同一业务动作可能修改多行、多个表；同一页面的一个数字也可能来自数次查询或 View。需求应分别定义成功、不完整失败和并发情形，并列出跨社区、跨账期、多条件筛选等典型读模式。

### 2.3.1 需保持一致的状态变更

入住办理牵涉 Person、Residence 和房产当前状态；车证审核牵涉申请状态、资格复核与许可证生成；通知发送产生主记录及多个收件关系；文件替换跨数据库与 MinIO。对前三者需用数据库事务和条件更新/锁控制，不允许“后一步失败、前一步却留下半完成状态”；文件跨存储不具数据库式原子性，须另讲提交后删除和失败补偿。

证据与图：ResidenceManagementServiceImpl.java、ParkingPermitServiceImpl.java、NotificationServiceImpl.java、FloorPlanService.java；2026-08-30 回滚证据；建议状态步骤与失败点时间线。

### 2.3.2 分页、统计和下钻

房产、人员、租赁、账单使用包含搜索、状态过滤、社区过滤和真实 COUNT＋LIMIT 分页；Dashboard 与年度/月度报表需要跨表聚合；月度统计还要从年份和月份下钻到社区及明细。未出账月份“无记录”不同于实际出账 0 元，查询结果和图表都要保留这一区别。后续第五章说明索引/View，第七章说明 Mapper/API 实现。

证据与图：stage1/PropertyMapper.xml、StatisticsMapper.xml、DashboardMapper.xml、AnnualStatisticsView.vue；建议“页面指标→查询来源”表。

## 2.4 非功能要求与规模口径

除功能外还要给数据库一个可以被检验的质量目标：坏数据应被约束拒绝、员工不能跨社区读写、备份后能恢复、结构变化有版本、查询在现有演示规模下有合理访问路径。这里不预设“高并发生产级”或“真实大数据”目标，避免第九章无证可证。

### 2.4.1 一致性、隔离、性能与恢复

历史审计确认 2026-09-01 快照约有 528 房、606 人、1584 表计、15840 读数、3696 账单、18273 账单项、3324 付款；这些行由关联键组织，不只是一个大表填数。它们足以让分页、聚合和索引选择具有观察意义，却不足以证明互联网生产吞吐。数据库备份与 MinIO 对象需要相互核对，容器重建不得等于数据消失；第八、九章将用已有 Runbook 验证。

证据与图：docs/PPT_FINAL_STATE_AUDIT.md、docs/DOCKER_STAGE1_RUNBOOK.md、docs/STAGE_G_SQL_PERFORMANCE_AUDIT.md；建议规模表加数据来源日期。完整批量造数脚本未找到，不能编造随机生成算法。

# 第三章 数据库概念结构设计

本章先画现实世界里的实体和联系，不画主键、外键、字段堆栈或 Mapper 方法。全局图让读者看到主线，局部图解释复杂关系属性；到章末才把“需要独立成为表”的联系送往逻辑设计。注意概念图与已经存在的 PPT 图只是表达工具，业务语义仍以源码/最终结构交叉验证。

## 3.1 建模方法与全局概念图

将业务按空间资产、人房、账务计量、租赁授权、停车、通知服务和文件对象划分子域，再用房产作为可管理实体的连接锚点。几十个实体一次铺在一张图上会使联系属性和基数难以讲清，所以主图只保留核心实体和主关系，子图展开时态、账务和中间关系。

### 3.1.1 全局图只回答“有什么业务对象、怎样相关”

全局概念 ER 的阅读顺序可从 Community→Building→BuildingUnit→PropertyUnit 开始，再看 Person 与房产的拥有/居住，房产到账单/表计/挂牌，Person 到通知/车辆等。它不应写 SQL 字段或实现表，否则读者会把概念联系误认成具体外键。更细的收件状态、付款明细和车证审核转入子 ER。

证据与图：docs/ppt_assets/global_er_concept.html、docs/PPT_ER_MODEL_AUDIT.md；建议直接放当前已校订的全局概念图并给图注“概念层，非物理表图”。

## 3.2 空间资产子域

四层资产体现物业对象从行政管理范围到可收费、可入住的最小房产。概念层应明确每一级的存在理由：小区用于管理范围，楼栋/单元用于实际位置与查询分层，房产用于人房、计费与租赁绑定。

### 3.2.1 Community→Building→BuildingUnit→PropertyUnit

一个社区有多栋楼，一栋楼有多个单元，一个单元有多套房。各层不是把地址字符串拆成四列：独立实体使社区授权、楼栋坐标、单元编号、房产账单分别有稳定锚点。PropertyUnit 的业务边界是整套房，系统没有按卧室逐间挂牌的 Room 表，不能在报告中凭 UI 户型图误加 Room 实体。

证据与图：V1 DDL、database/tests/05_seed_assertions.sql A14、docs/ppt-er/01_global_core_er.html；建议四层局部 ER、标明 1:N 基数。

### 3.2.2 户型模板与物理房产不是同一对象

多套房可共享相同 LayoutTemplate，模板图也可供多套房复用；后续面积变体让同一布局在不同面积段展示不同图片。若每套房复制一张图片，文件数量与维护成本随房产规模线性增加。概念层可画“房产采用模板、模板关联图”，物理细节与 MinIO 留第七章。

证据与图：V3/V9/V10 与 docs/STAGE_UI_R4_FLOORPLAN_VARIANT_PREVIEW_FIX_REPORT.md；建议模板→默认图/面积变体的小图。

## 3.3 人员、产权与居住子域

Person 是现实主体，Ownership 和 Residence 是不同的“人与房产之间发生过什么”；两者都能带起止日期，不能用一个模糊的 person_property 表混装。状态快照与事实历史的差异，是后续唯一约束和并发更新的起点。

### 3.3.1 Ownership 与 Residence 的关系属性

产权联系带份额、证书与生效/失效时间；居住联系带角色、入住/迁出时间，并在后续增加预期结束日。业主不一定住在该房产，住户也不一定拥有产权，因此“住户就是业主”不成立。ER 子图应把关系实体化，而不是让 Person 或 PropertyUnit 直接存若干重复列。

证据与图：V1 ownership/residence，V7 居住预期结束日，docs/ppt-er/03_person_ownership_residence_er.html；建议一张人房双联系及时间属性图。

### 3.3.2 当前关系与历史关系

结束日期为空可表示当前有效关系，结束后的行继续保留历史。最终备份还通过生成列和唯一键控制某些当前态，但“产权规则”曾由 V3 的一房一个当前人演化为最终的同一人房至多一条当前记录；概念图只需表达“当前/历史”语义，物理唯一的具体口径在第五章按最终备份讲。不得用早期迁移的约束解释最终系统。

证据与图：V3、2026-09-01 备份 ownership/residence DDL、docs/report-archaeology/REPORT_SECOND_PASS_HIGHLIGHT_AUDIT.md；建议并排的“阶段规则/最终规则”对照，不在 ER 图中硬写唯一索引。

## 3.4 收费与计量子域

此子域至少区分“计价规则”“应收依据”“付款事实”“用量来源”。拆分不是为凑表数量：不同事实拥有不同生命周期和键，错误合并会造成重复列、历史改写和部分支付口径不清。

### 3.4.1 FeeType、FeeStandard、Bill、BillItem、Payment

FeeType 是费用类别，FeeStandard 是按社区/类型/起始时间适用的价格；Bill 标识某房某账期，BillItem 标识账单中的一条费用，Payment 表示后续一次实收尝试/事实。一张账单包含多条明细，也可有多条支付记录；因此不能仅靠一个总 amount 和 paid 布尔值描述账务。概念子图可先示基数与时间关系，不在此讨论具体 View SQL。

证据与图：V1 表定义、docs/ppt-er/04_billing_er.html；建议从费用规则和付款两侧汇聚至账单的业务 ER。

### 3.4.2 Meter 与 MeterReading 的设备—历史分离

Meter 是房产上的计量设备，MeterReading 是按时间累计的读数；若只在 Meter 存最新值，就无法复核任一账单对应的前后读数。一个设备可有多条读数，且相同时刻不应重复记录。概念层将其定义为 1:N；唯一键与模拟器幂等实现留第五、七章。

证据与图：V1 meter/meter_reading、backend/src/main/resources/mapper/meter/MeterSimulationMapper.xml；建议设备到时序记录的局部图。

## 3.5 其他需要实体化的业务联系

租赁、停车、通知等不应平均逐表讲述，而应选“联系本身有属性或历史”的例子。每个小图只解决一个问题：谁授权谁、谁收到了什么、申请和许可证为何不是同一个状态字段。

### 3.5.1 授权、挂牌和合同的不同阶段

业主对机构/房产的 Authorization 与面向市场的 RentalListing 是两个事实；挂牌结束不应改写原授权，后者可指向特定授权。合同和参与人表属于模型范围，但最终备份的合同数据为空，本稿只从概念上说明预留的签约关系，不将其写为当前已验证的完整签约流程。

证据与图：V1 authorization/rental_listing/lease_contract/contract_participation、2026-09-01 备份；建议“授权→挂牌→合同”局部图，并以虚实线区分已验收页面与数据结构预留。

### 3.5.2 车证与通知中的“关系也有自己的状态”

Notification 与 Person 的多对多通过 NotificationRecipient 实体化，TO/CC、投递和已读都属于具体收件关系。停车方面，人员与车位的使用权、车辆登记、居住资格、申请和已发许可证并非一张表的状态切换；最终车位和车位权利数据为空，报告应重点写已能核实的车证申请/审核链，不夸大车位管理数据。概念图要呈现事实拆分，不用伪 ER 框画发送范围。

证据与图：V1/V4/V8、docs/ppt-er/05_vehicle_parking_permit_er.html、06_resident_service_notification_er.html；建议两张小图，不把通知范围条件画成持久化实体。

# 第四章 数据库逻辑结构设计与范式分析

本章把第三章的实体和联系转成关系模式，说明键的选择、函数依赖与更新异常；不能将“用了代理主键”当成范式证明。讨论初版 V1 的范式材料时需标记版本，最终增加的表和生成列另述，不把 25 表的历史结论扩大成全部 46 张主要表。

## 4.1 ER 到关系模式的转换

一对多通过子表外键落地，多对多通过关联关系落地；若联系有时间、份额、角色或审核状态，关联关系不是简单的两个 ID，而是独立业务事实。此节可先给几条简写关系模式，再把完整字段字典移到附录。

### 4.1.1 层级外键与时态联系表

Building(…community_id)、BuildingUnit(…building_id)、PropertyUnit(…building_unit_id) 用父键维持层级。Ownership 和 Residence 分别由 person_id 与 property_unit_id 连接，同时拥有 start_date/end_date 和自己的代理主键；业务键中还包含起始日以保留多次关系历史。参照完整性使“指向不存在房产的人房关系”被数据库拒绝，而不是靠页面检查。

证据与图：V1 对应 DDL，docs/database/RELATION_SCHEMA_V1.md；建议“ER 联系→关系模式→外键”三列表。

### 4.1.2 弱实体、三元授权与收件关系

BillItem 的标识是 (bill_id,item_no)：局部编号离开账单没有意义。Authorization 的业务事实由人员、房产、机构及开始日期共同区分；NotificationRecipient 由通知与具体 Person 构成并承载收件角色和状态。若将这些值放入主表的逗号列表或多个 recipient1/recipient2 列，无法维护原子性、基数与逐人状态。

证据与图：V1 bill_item/authorization、V4 notification_recipient、docs/database/NORMALIZATION_REPORT_V1.md；可放三个“联系实体化”简式对照。

## 4.2 候选键与函数依赖的代表性推导

正文挑能解释设计决定的 4～5 张表，不逐表套“满足 3NF”的固定句式。业务候选键、数据库代理主键与自然唯一约束须分清；某些派生/快照字段是有理由的物理实现，但也应讨论它对严格范式判断的影响。

### 4.2.1 空间层级与产权/居住的依赖

property_unit_id 决定本房的静态属性和 building_unit_id；building_unit_id 再经 building_id、community_id 决定社区名称，因此在房产表重复 community_name 会形成可避免的传递依赖。Ownership 与 Residence 的业务键可分析为 (person_id,property_unit_id,start_date)，分别决定份额/证书/终止时间和居住角色/终止时间；这些属性不由单一 person_id 或 property_unit_id 决定。

证据与图：V1 唯一键、docs/database/NORMALIZATION_REPORT_V1.md；建议函数依赖箭头图，并明示最终产权当前唯一键另见第五章。

### 4.2.2 账单与账单项的局部依赖

Bill 中 (property_unit_id,billing_year,billing_month) 唯一确定账单，账单总额由多条 BillItem 聚合而非独立手填。BillItem 中 (bill_id,item_no) 决定 fee_standard_id、读数、quantity、出账时 unit_price；金额由 quantity×unit_price 生成。FeeType 名称留在 FeeType，社区费标留在 FeeStandard，避免一个账单项包含重复且易过期的说明性字段。

证据与图：V1 bill/bill_item/fee_standard 的 UK、生成列；建议“账单项业务键与非键属性”表。需在 4.3 讨论生成金额是派生列，不能对严格 3NF 做无条件完美宣称。

### 4.2.3 账号与社区范围的复合键

account_community_scope 以 (account_id,community_id) 为主键，status、assigned_at、assigned_by_account_id 描述该账号对该社区的一条授权记录。单凭 account_id 不能决定唯一 community_id，反过来也一样；把社区 ID 列表存进 app_account 会破坏原子性、难以关联 community 并使撤销某个社区范围困难。该关系同时是第六章 SQL DataScope 的数据基础。

证据与图：V4 scope 表、backend/src/main/resources/mapper/access/AccessControlMapper.xml；建议账号—作用域—社区关系模式示意。

## 4.3 1NF、2NF、3NF 与工程折中

范式分析要指出预期避免的插入、修改、删除异常，而非只写定义。初版范式报告对 25 张表做了总体判断，本报告应复核最能体现复合键的样本，并说明后续字段/生成列改变后为什么不能直接搬用初版“全部满足某范式”的结论。

### 4.3.1 原子性与复合键完整依赖

账单项目、人员参与合同、社区授权和房内设施不存逗号分隔的多值属性，体现 1NF；对 BillItem、Ownership、Residence 等复合业务键，应判断份额、角色、计价记录依赖完整联系而非键的一部分。若把小区名复制到 PropertyUnit、费种名复制到 BillItem，会出现更新异常，可用“更名/调价”反例解释 3NF 动机。

证据与图：docs/database/NORMALIZATION_REPORT_V1.md、V1 DDL；建议一张“错误大表→拆分后关系→避免的异常”表，不机械罗列每张表。

### 4.3.2 快照、生成值和 JSON 的审慎表述

BillItem 保留出账时 unit_price 快照是历史正确性要求，尽管 FeeStandard 另有当前标准；amount 为数据库生成并储存的派生值，有 (quantity,unit_price)→amount 的依赖，正文应将其作为受控物理派生而非忽略。NotificationRecipient 的 payload_json 是面向特定收件人的消息上下文，不应拿它替代 Person/Bill 等可查询关系。对这些折中要讲“为何保存、如何维护一致、不能用于什么查询”，而不是泛称反范式或泛称完全 3NF。

证据与图：V1 bill_item，V5 payload_json，ChargeNoticeComposer.java；建议“原始事实/快照/派生数据/消息载荷”四类表。

## 4.4 数据字典与模式命名

数据字典的价值在于统一业务含义、取值、键、空值和时间语义；整页 CREATE TABLE 不等于解释。正文精选房产、Person、Ownership、Residence、Bill、BillItem、Payment、scope、notification_recipient、file_object 等，其他表入附录，注明最终库版本。

### 4.4.1 核心表字典与附录分工

每张正文核心表只列“字段/含义/约束/为何必要/来源版本”，示例说明 residence.end_date=NULL 与“居住历史仍在”、bill_item.amount 为计算值、account_community_scope.status 可使范围失效。附录再给最终 46 张主要表的索引与外键摘要，并与 V1 的 25 表初始字典分开；缺少创建迁移的表明确标“最终备份有结构、迁移来源未证实”。

证据与图：2026-09-01 备份、docs/database/RELATION_SCHEMA_V1.md；建议一页核心表字典样张，不截图几十页 DDL。

# 第五章 数据库完整性、约束、索引、视图与物理设计

本章是数据库设计核心。每条规则都要回答“若无此约束会产生什么非法数据”，每个索引都要回答“哪类访问路径需要它”，每个 View 都要说明底表、口径与空值处理。V1/V3 阶段规则与最终备份冲突时，以最终状态说明现状，阶段规则仅用于演进分析。

## 5.1 实体、参照与用户定义完整性

PRIMARY KEY 保证记录可识别，FOREIGN KEY 约束实体之间的合法连接，UNIQUE/CHECK 保护具体业务规则；应用层校验与数据库约束共同工作，而非互相替代。可选约束正反例从 V1 NEG-01～16 精选，不把历史隔离库测试当成当前全量回归。

### 5.1.1 主键与业务唯一键

如 bill_id 是物理主键，bill_no 是业务编号唯一键，(property_unit_id,billing_year,billing_month) 防止同房同账期重复账单；(meter_id,reading_at) 防止同表同一抄表时刻重复读数；BillItem 使用 (bill_id,item_no) 复合主键。对每个键要指出它防止的重复事实，而不只展示 CREATE TABLE 语法。

证据与图：V1 bill/meter_reading/bill_item，database/tests/03_constraint_negative_cases.sql；建议“错误插入→哪个唯一键拒绝”矩阵。

### 5.1.2 外键、复合外键与删除策略

rental_listing 的 (authorization_id,property_unit_id) 复合外键要求挂牌引用的授权确实属于该房产，单列 authorization_id FK 无法在数据库层阻止错配。人房、账单、读数等历史关键事实多处使用 RESTRICT，不允许删除父行让历史失去对象。部分纯授权关联使用 CASCADE 或 SET NULL，应具体核对其语义，不概括说“全库禁止级联”。

证据与图：V1 fk_listing_authorization_property、V4 account_community_scope 的外键动作、最终备份；建议复合 FK 与错误配对反例图。

### 5.1.3 CHECK、状态组合和时间边界

产权份额、账期月份、读数非负、车证月份/金额、授权模式与挂牌发布者组合等，均可由 CHECK 阻止明显非法写入。尤其当前最终 ownership 的份额规则为大于 0 且不超过 1，不再是 V3 阶段等于 1；报告应同时给 DDL 和对应负例。CHECK 不自动实现跨多行的份额合计或时段不重叠，这些边界必须明说。

证据与图：V1/V8、2026-09-01 备份 ownership、database/evidence/07_negative_cases.txt；建议一页“规则—DDL—NEG 编号—不能保证什么”表。

## 5.2 当前态与历史态的物理约束

“只有一个当前有效关系”不能仅用普通 UNIQUE(person_id,property_id,end_date) 推导，因为 MySQL 对多个 NULL 的唯一处理不同于直观想象。最终库用生成列将满足当前态条件的键值写出，历史行产生 NULL，再用唯一索引限制当前态；这是重要设计，也暴露出迁移来源缺口。

### 5.2.1 最终 Schema 中的生成列唯一

最终备份中 residence.current_resident_person_id 约束一个人当前最多一条居住，current_responsible_property_unit_id 约束房产当前最多一个责任住户；rental_listing.current_published_property_unit_id 约束一房最多一个 PUBLISHED 挂牌；agency_staff.current_staff_person_id 限制一个人当前最多归属一家中介。每项按 CASE 条件、唯一键和允许多条历史行三步解释，而非泛称“所有当前状态都唯一”。

证据与图：2026-09-01 备份中对应四表 DDL；建议一张“当前行生成 ID / 历史行生成 NULL”双行示例。部分生成键在现存 V1–V10 中未见来源，需交叉引第八章。

### 5.2.2 产权规则必须以最终备份为准

V3 的 ck_ownership_single_owner_share 与 uk_ownership_one_current_owner 曾限制一房一当前产权人；最终备份是份额范围 CHECK 与 (person_id,current_property_unit_id) 唯一，禁止“同一人对同一房重复当前关系”，并不禁止不同人同时有当前产权。正文不能写成最终库仍是一房一个业主，也不能由此推断数据库已保证共有份额合计为 1。原变更迁移尚未找到，应把事实差异当作第八章迁移纪律反思，而非在此自造理由。

证据与图：V3__phase2a_security_assets_and_files.sql 第 124–129 行、2026-09-01 备份 ownership；建议“阶段 DDL→最终 DDL→规则变化”对照表。

## 5.3 查询模式驱动的索引设计

索引不是越多越好：主键、唯一键和外键访问与业务筛选的组合索引各有目的。可把 WHERE 等值列、范围列、排序列的次序同实际查询一起分析，再以 EXPLAIN ANALYZE 记录访问路径；对当前规模没有证据的瓶颈不盲加宽索引。

### 5.3.1 多社区范围与层级 JOIN

account_community_scope 的主键以 account_id 为前缀，适合从当前账号取 1～多条有效社区 ID；空间层级的社区/楼栋/单元/房产唯一键支持逐级关联。Stage G 抽样中 scope 查询为索引查找，两个社区只返回少量行。这比“给所有 community_id 建索引”更具解释性：要看查询从哪张表、按何列进入。

证据与图：V4 scope PK、V1 层级 UK、docs/STAGE_G_SQL_PERFORMANCE_AUDIT.md 的 SQL-01/07；建议账号→Scope 与层级 JOIN 的访问计划小图。

### 5.3.2 账期状态联合索引的前后对比

V1 的 uk_bill_property_period 以 property_unit_id 开头，适合定位某房某账期，却不等于跨房产按 year/month/status 找账单的最佳前缀。V6 增 idx_bill_period_status(billing_year,billing_month,status,bill_id)；历史 EXPLAIN 场景从约 603 行扫描到 11 行索引查找，时间约 0.85ms→0.13ms。应注明这是本机小数据瞬时样本，仅用于说明访问路径变化，不可概括为生产系统固定 6 倍提速。

证据与图：V1/V6、docs/STAGE_G_SQL_PERFORMANCE_AUDIT.md；建议并排 EXPLAIN 截图加原始 SQL 和过滤条件。

### 5.3.3 分页、排序与未完成优化

PropertyMapper 的 count/page 共用过滤片段，LIMIT/OFFSET 支持真实分页；资产旧链的排序用 MyBatis choose 白名单，避免任意 ORDER BY 标识符注入。OFFSET 很深时可能仍需扫描/跳过大量行；目前没有游标分页及深页压测证据，应在正文给出这一潜在边界，而不把现有数百房产表现外推到百万行。

证据与图：stage1/PropertyMapper.xml、asset/PropertyUnitMapper.xml、docs/STAGE_G_INDEX_AUDIT.md；可放“同一 where 片段支撑 COUNT/页查询”代码局部。

## 5.4 财务 View 与聚合口径

View 是对底表查询逻辑的封装，不是物理保存的新业务事实。重点用最终备份末尾 Final view structure，而不是 mysqldump 中间为恢复依赖建立的临时 1 AS 占位 View；这一区分本身可作为数据库备份格式识读示例。

### 5.4.1 单账单应收、实收与欠费

v_bill_financial_summary 以 bill 为主表，LEFT JOIN 账单项按 bill_id 的 SUM(amount)，再 LEFT JOIN status='SUCCESS' 的 payment SUM(amount)，COALESCE 使未关联时仍保留账单并把金额视为 0；outstanding_amount 按 GREATEST(应收－实收,0) 截至非负并 ROUND。若改 INNER JOIN，尚未付款的账单可能消失，欠费统计随之失真。必须说明此 View 未表达项目级付款分摊。

证据与图：2026-09-01 备份末尾 v_bill_financial_summary Final view、BillMapper.xml；建议视图依赖图、未付/部分支付两行手算表。

### 5.4.2 月度 View 与统计页面

v_monthly_collection_summary 按账期年/月从单账单 View 汇总 bill_count、应收、实收、欠费；收缴率是 SUM(实收)/NULLIF(SUM(应收),0)×100 后取两位，避免除零。未存在账单的月份没有 View 行，年度 Vue 图的 1–12 月轴可补时间维度但指标必须为 null，不可造 0 元记录。统计 SQL、View、前端分别承担事实、聚合、展示语义。

证据与图：最终备份月度 View、stage1/StatisticsMapper.xml、frontend/src/views/statistics/AnnualStatisticsView.vue；建议“有账单但金额 0”与“无任何账单”图例。

## 5.5 类型、编码与物理存储边界

金额使用 DECIMAL 而非浮点；时间可区分日期、时刻与更新戳；utf8mb4 支持真实中文物业名称，外键约束由 InnoDB 落实。文件内容不写 BLOB 到核心房产表，file_object 保存可查询元数据而 MinIO 保存对象。这些选型有设计意义，但不宜展开成产品安装教程。

### 5.5.1 字段类型与跨存储位置

BillItem 的 quantity/price/amount 保留明确精度，amount 由生成表达式维护；经纬度存于社区/楼栋普通数值列，spatial_boundary 为 JSON 类型，不存在已证实的 MySQL POINT/SPATIAL INDEX 实现；文件 hash、bucket 和 key 支持元数据与 MinIO 对账。Docker 恢复中的中文抽样可证明当时编码恢复正确，不能只凭 DEFAULT CHARSET 推断所有链路永无乱码。

证据与图：V1/V3/V4、最终备份、docs/DOCKER_STAGE1_RUNBOOK.md；建议“关系字段类型与大对象去向”示意，地图只作数据落库配角。

# 第六章 多小区权限与数据范围控制

本章回答老师问过的“多小区体现在哪儿”。一个数据库中有多个 Community 只是存储事实；多小区系统的难点是相同功能权限下，不同账号能读写不同的社区集合，并能阻断绕过菜单的直接 API 访问。讲述顺序应为作用域关系→授权取值→列表 SQL→对象级验证→实际账号对照。

## 6.1 账号与社区的多对多授权模型

账号不是只能绑一个 community_id：V4 创建 account_community_scope，以 (account_id,community_id) 为主键，附状态、授予人和时间；一条记录即一社区范围，多条即多社区范围。SYSTEM_ADMIN 的全局分支与普通员工的 Scope 集合不同，不应为全局账号伪造所有社区授权行。

### 6.1.1 Role、Permission 与 Scope 的职责

account_global_role/role_permission 决定此人能否调用房产、账单或通知等功能；account_community_scope 决定在获准功能内哪些行属于其范围。若仅在菜单层隐藏页面，直接请求 /api/admin/properties 仍可能越权；若只靠社区范围却不查 Permission，拥有某社区关联的人可能操作不应有的业务。二者在 Service/Mapper 中需要组合，而不是相互替代。

证据与图：V3/V4 身份与权限结构，backend/src/main/java/com/smartproperty/platform/access/ 中 AuthorizationService、DataScopeServiceImpl；建议“功能权限轴×数据范围轴”二维图。

### 6.1.2 作用域的有效与空集合

AccessControlMapper.xml 从当前账号取 ACTIVE 授权社区；某员工若没有有效社区，查询应得到空集或拒绝，而非省略 WHERE 变成全库。管理端 Mapper XML 的动态 IN 使用 foreach，空范围分支有 AND 1=0 等封口设计。本文应展示实际 SQL 片段及其参数绑定，说明“空集合的安全默认值”比前端下拉框限制更可靠。

证据与图：backend/src/main/resources/mapper/access/AccessControlMapper.xml、stage1/PropertyMapper.xml、stage1/StatisticsMapper.xml；建议三组 SQL：global、多 scope、空 scope。

## 6.2 列表级与对象级控制

列表权限约束每页能查到哪些行；详情、修改和审核还需把传入对象 ID 反查到真实 Community。前端传入的 communityId 不能作可信授权依据，因为请求参数可以被改写。这个差别应配合“房产 ID 越权访问”的思维实验说明。

### 6.2.1 沿业务外键反查真实社区

房产从 PropertyUnit→BuildingUnit→Building→Community 回溯，账单先找所属 PropertyUnit，居住关系也找所在房产；DataScopeServiceImpl 的 requireProperty/requireBill/requireResidence/requireBuilding 等据此拒绝不在 Scope 的对象。写操作如入住、审核不能只用列表 SQL 过滤，还需在进入变更前校验目标房产。第七章具体展示一条调用链。

证据与图：DataScopeServiceImpl.java、AccessControlMapper.xml、ResidenceManagementServiceImpl.java、ParkingPermitServiceImpl.java；建议“用户提交 billId→反查 property→community→scope 判断”流程图。

### 6.2.2 遗留 API 与前端身份分流

系统同时保留 /api/v1 资产链、/api/admin 管理链与 /api/resident/me 本人链；审计不能只查新接口忽视旧路由。Stage H-R1 文档记载居民直连旧房产接口为 403，物业账号旧房产查询与社区范围一致。本文需解释这曾是修复范围而非天然正确性，展示当前 Security、Service 和 Mapper 的实际封口位置。

证据与图：docs/STAGE_H_R1_IDENTITY_SCOPE_SECURITY_REPAIR_REPORT.md、frontend/src/router/index.js、backend/src/main/java/com/smartproperty/platform/asset/controller/PropertyUnitController.java；建议角色/API/数据域表。

## 6.3 两个账号的可解释对照

用已记录的真实验收账号替代抽象“管理员甲乙”：一个社区与两个社区的员工拥有可比较的物业功能，但返回房产数量不同。演示时从数据库 Scope 行追到权限 SQL，再追到页面数值；避免把不同日期或不同库的截图放在同一对照表。

### 6.3.1 从授权行到 72 与 168 套房

Stage H-R1 报告记载 staff_scope_a 仅见一个社区与 72 套房，staff_scope_c 见两个社区与 168 套房；Dashboard 房产总数也分别为 72/168。这一组证据同时检验关系表、列表 SQL 和统计 SQL 是否采用同一 Scope。正式写作应注明这是当时测试快照，并附 account_community_scope 的脱敏 SQL 结果、接口返回及日志 Trace；不能仅展示网页数字。

证据与图：docs/STAGE_H_R1_IDENTITY_SCOPE_SECURITY_REPAIR_REPORT.md 第 54–59 行，AccessControlMapper.xml、PropertyMapper.xml、DashboardMapper.xml；建议四栏“账号→Scope 行→API 总数→页面卡片”。

### 6.3.2 安全主张的合理边界

已有场景证明关键路径和遗留入口的具体拒绝/过滤，但不等于证明未来新增每个 API 都已自动继承 Scope。系统还有管理员全局分支、居民本人分支和中介模型，应按现有实现分别陈述。报告可把“增加接口时必须做 Permission＋Scope＋对象验证”写成工程约束，而不是“数据库自身提供了原生行级安全”——当前实现主要在应用服务和 Mapper SQL。

证据与图：SecurityConfig.java、DataScopeServiceImpl.java、Stage H-R1 报告；建议“已验证/当前实现/未穷尽路径”边界框。

# 第七章 关键业务数据库实现与 MyBatis 数据访问设计

本章让前述关系设计可在真实业务页面中被追踪。每个案例尽量按页面动作→HTTP→Controller→Service→Permission/Scope→Mapper 接口→XML id→底表/View→VO→页面指标讲，不粘贴整页代码。必须区分“字段在数据库存在”“读接口已实现”“写流程已验证”三种成熟度。

## 7.1 MyBatis XML 的组织与安全参数

项目的复杂 JOIN、筛选和统计留在 XML，不在 Controller 拼 SQL。接口层确定 Query/DTO/VO 的职责：Query 表示筛选入参，实体/Row 对应数据库取值，VO 是对外响应；不要把 Entity 的所有字段原样返回给前端，尤其不应泄露私密字段。可选一条房产分页链阐释这些约定。

### 7.1.1 可复用片段与动态条件

PropertyMapper.xml 中 countProperties 与 selectPropertyPage 复用 propertyFrom/propertyWhere 等 sql id 和 include refid，使 COUNT 与实际列表采用同一过滤条件；if 只在用户提供过滤项时拼条件，foreach 把一组社区 ID 放进 IN，choose/when/otherwise 用于受控分支，资产旧链还将 ORDER BY 控制在白名单。需要指出：这些是生成 SQL 结构，参数值仍通过 #{} 绑定。

证据与图：backend/src/main/resources/mapper/stage1/PropertyMapper.xml、mapper/asset/PropertyUnitMapper.xml；建议并排给 XML 片段及最终 SQL，不粘整个文件。

### 7.1.2 PreparedStatement 的问号与真实参数

日志里的 ? 是 JDBC 预编译占位，不是“SQL 少了值”；MyBatis #{} 将用户值作为参数传入，避免直接把文本拼成 SQL 字面量。动态 IN 的 ID 仍由权限上下文提供，不能因 #{} 安全就省略 Scope 校验。观察日志时应脱敏姓名、手机号、凭据，展示可定位的 page/year/propertyUnitId 即可。

证据与图：mapper XML、docs/DOCKER_STAGE2_5_OBSERVABILITY_RUNBOOK.md、既有 logs/run-*；建议一行“XML 条件→带 ? 的 SQL→安全参数”图。

## 7.2 房产分页与人员居住查询

这两条读链便于展示“一个页面为什么涉及多表”和“分页为什么要 COUNT 与 SELECT 配套”。不要把旧资产工作台 /api/v1/properties 与新管理端 /api/admin/properties 合并为一条接口。

### 7.2.1 房产分页的 Count、排序与层级联查

PropertyView.vue 调 /admin/properties，经 PropertyController→PropertyServiceImpl→PropertyMapper.countProperties/selectPropertyPage；WHERE 可包含房号、小区、楼栋、状态等，JOIN 取得完整层级，COUNT 返回总数，LIMIT/OFFSET 返回当页。前端根据 total/pageSize 展示页数。可举“第 2 页每页 20 条”的具体 SQL 参数说明，但不填未经核对的当日结果。

证据与图：frontend/src/views/property/PropertyView.vue、frontend/src/api/property.js、property/PropertyController.java、mapper/stage1/PropertyMapper.xml；建议同请求的总数/当页 SQL 与分页条截图。

### 7.2.2 人员与居住不是直接扫 Person

ResidentView.vue 的分页由 ResidentController→ResidentServiceImpl→ResidentMapper.countResidents/selectResidentPage/selectResidentDetail 支撑，需从 residence 关联 person、property_unit 及空间父链，才能回答“这个人住哪一套、属于哪个社区、当前是什么关系”。手机号等个人信息在展示层/服务响应处理时需要脱敏，正式报告截图也要脱敏；不能因为数据库存有联系方式就全部印在答辩材料。

证据与图：frontend/src/views/resident/ResidentView.vue、mapper/stageb/ResidentMapper.xml、相应 VO/Service；建议一条 SELECT JOIN 与其页面表格对应标注。

## 7.3 账单、统计与 Dashboard

统计页面不是用随机数凑图，而是由多条针对性 SQL 分别提供指标。正文宜先选一个“账单行如何得出欠费”，再追“月度趋势如何按账期聚合”，避免一次贴几十条 Mapper 导致读者看不出每条 SQL 的业务语义。

### 7.3.1 账单明细与付款回查

BillView.vue 调 /admin/bills、/{billId}，BillController→BillServiceImpl→BillMapper.countBills/selectBillPage/selectBillDetail/selectBillItems/selectPayments。BillItem 中 unit_price 和 generated amount 是应收依据，Payment 的 SUCCESS 汇总是实收事实，View 统一余额口径；未付款仍要出现账单。可选一张部分支付账单从行、项目、支付到页面卡片做手算，注明 payment 未给出收费项目分摊。

证据与图：frontend/src/views/billing/BillView.vue、mapper/stage1/BillMapper.xml、最终财务 View；建议“原表三组行→单账单 View→页面”追踪表。

### 7.3.2 Dashboard 与月年报表各自承担什么

DashboardView.vue 并行请求 /admin/dashboard/overview、monthly-collection、fee-composition、bill-status、occupancy、overdue-top、residence、rentals 等真实 API；DashboardMapper 与 StatisticsMapper 分别做概览、分布及时间聚合。年度页 /admin/statistics/annual 用数据库实有月份生成指标，Vue 仅铺 1–12 月横轴并以 null 保留缺月份；月度页的社区下钻再请求 /monthly/communities、/monthly/drilldown。应把某张卡或图固定到 XML id 和底表/View，不把所有图笼统归为同一 SQL。

证据与图：DashboardView.vue、MonthlyStatisticsView.vue、AnnualStatisticsView.vue、dashboard/DashboardController.java、statistics/StatisticsController.java、mapper/stage1/DashboardMapper.xml/StatisticsMapper.xml；建议“图表→API→XML id→底表”精选 4 行。

## 7.4 写链路：入住、通知与车证

写操作更能暴露数据库设计是否可靠。此节只挑三个有独立事实表或并发边界的业务，不把所有 CRUD 都当重点；第九章再用失败记录检验它们。

### 7.4.1 入住与搬出：人房关系和房产状态一致性

MoveInView.vue 请求 /admin/residence-management/move-ins，ResidenceManagementController→ResidenceManagementServiceImpl→ResidenceManagementMapper；服务先验证身份/房屋权限和可入住条件，再在事务中新增 Residence、条件更新房产状态/版本。搬出结束当前居住关系并更新房产状态。这里需展示一次成功和一次条件冲突时数据库行的预期差别，强调端点返回 200 不等于每个中间 SQL 都可独立提交。

证据与图：MoveInView.vue、ResidenceManagementServiceImpl.java、ResidenceManagementMapper.xml、docs/STAGE_H_IMPLEMENTATION_REPORT.md；建议事务时序图与房产 version 前后值。

### 7.4.2 通知发送的 N:M 展开与欠费口径

NotificationView.vue 的发送请求进入 NotificationController→NotificationServiceImpl：根据 Scope 解析具体 Person，插入一条 Notification 和多条 NotificationRecipient；ChargeNoticeComposer 一次批量查询收件人应缴数据，按人组装 payload_json。部分缴费若无法准确分配到 fee item，就明确记录 unallocatedSelectedBillOutstanding，而不制造某费用类别的虚假剩余金额。通知主表的创建迁移来源缺失，本文在第八章交代。

证据与图：NotificationServiceImpl.java、ChargeNoticeComposer.java、mapper/notification/NotificationMapper.xml/ChargeNoticeMapper.xml、V4/V5；建议通知主记录与三条收件关系的样例表。

### 7.4.3 车证申请到审核发证

居民端先查本人车辆及房产资格，提交 parking_permit_application；管理端审核时 ParkingPermitServiceImpl 对 Person 与 PropertyUnit 执行 FOR UPDATE、复查资格与可用数量、仅将 PENDING 条件更新为 APPROVED，再在同一事务插 parking_permit。若把许可证直接写进车辆记录，会抹掉申请和拒绝历史。需同时指出申请前的 existsPendingApplication 并非数据库唯一约束，仍有并发重复申请边界；实际演示库车位/车位权利为空，也不代表车证完全依赖车位表。

证据与图：V8、parking/ParkingPermitMapper.xml、ParkingPermitServiceImpl.java、两端 ParkingPermitView.vue；建议“资格→申请→审核→发证”状态图。

## 7.5 文件、地图与表计：作为数据库设计的外延

这三项容易被写成技术展示，应重新问“关系数据库保存了什么、如何避免丢失历史或产生双份事实”。篇幅明显小于账务与权限。

### 7.5.1 户型图对象与关系元数据

房产专属图、户型默认图和面积变体按优先级解析；MySQL file_object 与模板/房产关联记录控制对象归属，MinIO 只存二进制。FloorPlanService 上传新对象、写数据库并在提交后删除旧对象，失败时尝试补偿删除新对象，但补偿可能失败，不能宣称分布式原子事务。最终备份 file_object 为 12、模板默认图 4、面积变体 12，关联行不能相加当作独立对象数。

证据与图：FloorPlanService.java、V3/V9/V10、docs/STAGE_E_MINIO_FLOORPLAN_AUDIT.md、Stage UI-R4 报告；建议对象键/元数据/业务关联三层图。

### 7.5.2 地图坐标落库与表计时序

高德用于地址解析和 JS 底图，社区/楼栋经纬度、坐标来源、POI ID、核验时间落回业务库；MapSceneMapper 联空间层级和 Scope 给前端 Marker，不能把第三方地图本身写成 MySQL GIS 能力。表计模拟器则读取每表最新历史读数，批量插入同分钟数据，(meter_id,reading_at) 唯一配合预查/INSERT IGNORE 保持同桶幂等；这说明“最新值”和“历史事件”分离。未发现 MySQL POINT/SPATIAL INDEX，不把地图列作空间索引案例。

证据与图：mapper/map/MapSceneMapper.xml、V3/V4、mapper/meter/MeterSimulationMapper.xml、docs/STAGE_H_IMPLEMENTATION_REPORT.md；建议两张小图，控制篇幅。

# 第八章 数据库演进、Flyway 版本管理与数据生命周期

本章用开发时间串联前面静态设计：当业务需求增加、已有数据无法随意清空时，Schema 变更必须有顺序、可核对、可恢复。Flyway 的贡献是减少版本漂移，而非自动解决所有历史手工 DDL；来源缺口必须正面写。最后从迁移过渡到备份与容器数据生命周期。

## 8.1 为什么数据库需要自己的版本历史

代码版本和数据库状态不能分离：某个 Java Mapper 引用了新列，而部署库仍是旧结构，应用就无法可靠启动/查询。Flyway schema history 让已执行版本及顺序可以核验；迁移应该记录需求变化、DDL、兼容处理和验证结果，不仅列出文件名。当前 Docker 启动配置默认关闭自动 Flyway，更需区分历史已迁移数据库与“每次启动执行脚本”。

### 8.1.1 V1–V3：基础模式、参考数据与安全/文件扩展

V1 建立 25 张核心表与主外键/业务约束；V2 加入可用的参考数据；V3 因账号安全、资产文件与审计等需求增加表、字段和当时的产权唯一规则。正文可用“原 Schema 对新需求不够→V3 改动→哪些 Service/页面获得新数据”来写，同时在脚注明示 V3 的产权限制不是最终快照规则。

证据与图：database/migrations/V1__init_schema.sql、V2__seed_reference_data.sql、V3__phase2a_security_assets_and_files.sql、docs/PRE_STAGE1_RECOVERY_REPORT.md；建议 V1→V3 时间轴。

### 8.1.2 V4–V6：多小区、通知与索引

V4 引入权限/作用域、边界结构并改造通知与收件人，V5 给每个收件关系增 payload_json 支撑个体化欠费通知，V6 针对账期状态检索增加联合索引。需求、结构和查询证据可以形成紧凑闭环：第六章 Scope 查询、第七章收费通知、第九章 EXPLAIN 分别引用这三版。V4 预设 notification 主表已存在，其原始创建迁移缺失，这是迁移完整性问题，不能隐藏。

证据与图：V4/V5/V6、docs/STAGE_G_SQL_PERFORMANCE_AUDIT.md；建议“需求→迁移→实际 SQL/页面→验证”矩阵。

### 8.1.3 V7–V10：居住、车证与户型图变体

V7 增居住预期结束日、居民报事等；V8 增资格、申请、许可证及权限，同时含演示 INSERT 数据，说明迁移与 seed 职责并未完全分离；V9 为 LayoutTemplate 增默认图，V10 继续按面积变体细化。版本不是为凑数字：每次都对应以前关系模型无法表达的新事实或复用需求。写作时对 V8 的演示 DML 单独标注，避免讲成纯结构迁移。

证据与图：V7/V8/V9/V10、docs/STAGE_UI_R4_FLOORPLAN_VARIANT_PREVIEW_FIX_REPORT.md；建议版本矩阵与 V9→V10 的数据关系小图。

## 8.2 迁移来源缺口与兼容反思

最有课程价值的反思不是“迁移永远完美”，而是发现最终快照与文件链不完全同构。只有把最终结构、历史脚本与恢复方式分开，才能准确判断哪些设计有来源、哪些改动可能在脚本外发生。这里不补造历史、更不在报告策划期修改数据库。

### 8.2.1 最终数据库与 V1–V10 不能画等号

2026-09-01 备份包含 agency_staff、notification 及 v_bill_financial_summary、v_monthly_collection_summary，但现存 V1–V10 未找到它们最初 CREATE 来源；V3 产权规则与最终备份冲突，最终 residence、挂牌、员工归属还出现若干迁移链中未定位来源的当前态生成列。可给出“结构存在、历史来源未定位”的四列表，而不是宣称迁移从零百分之百重建现有 46 表＋2 View。

证据与图：V1–V10 全链、最终备份、docs/report-archaeology/REPORT_SECOND_PASS_HIGHLIGHT_AUDIT.md；建议“迁移文件→最终 Schema 差异”审计表，列明待查证而不编造原因。

### 8.2.2 字段长度、既有数据与变更纪律

对已有行加 NOT NULL、CHECK、唯一键或扩大字段长度，都应先检查存量数据能否满足新规则，并有回滚/备份方案。本项目的阶段性材料常提到 ID/字段 32→64 扩展，但第二轮未找到明确字段、原错误和原始变更 SQL；本节只能以“一般迁移兼容性问题及本项目尚缺原证据”展开，不能写为已复原的真实修复案例。可用 V6 添加索引和 V9/V10 新表的确证变更作为正例。

证据与图：docs/report-archaeology/REPORT_SECOND_PASS_HIGHLIGHT_AUDIT.md、V6/V9/V10；建议“已证变化／口述待证变化”双色表。

## 8.3 备份、恢复与容器数据生命周期

迁移文件描述结构变化，完整 SQL 备份与 MinIO 冷备承担演示数据和对象恢复；二者职责不同。Docker named volume 让服务容器销毁/重建与数据卷生命周期分离。报告应该解释这条链如何验证已有数据，而不是把 docker compose up 成功等同于数据库恢复成功。

### 8.3.1 MySQL 与 MinIO 的恢复对账

Stage 1 Runbook 记录将 MySQL 备份恢复到 Docker，核对 Flyway 历史 V1–V10、业务表、两个 View、真实中文、FK/CHECK/索引；MinIO 按对象镜像方案恢复 property-assets，并抽样与 file_object 的对象键对应。Redis 不迁移旧数据，只检查服务可用性。这些证据说明数据库与非结构化对象需要双份保护，但不能声称 MySQL+MinIO 同一事务或已经建立长期异地灾备。

证据与图：docs/DOCKER_STAGE1_RUNBOOK.md、同级 deployment_backups 的 2026-09-01 SQL、docs/STAGE_E_MINIO_FLOORPLAN_AUDIT.md；建议“备份源→容器卷→核验项”表。

### 8.3.2 down/up 持久化与权限边界

Runbook 执行 compose down（不带 -v）后重新 up，检查 MySQL 行数与 MinIO 对象仍在，从而区分容器和数据的生命周期。compose.yaml 内部 DNS 例如 mysql:3306、minio:9000 使后端不依赖宿主机临时地址；数据库账号和凭据由本机配置注入，不展示口令。尚未找到能证明 DDL/DML 分权的 GRANT 导出，不在报告中宣称最小权限已达成。

证据与图：compose.yaml、docs/DOCKER_STAGE1_RUNBOOK.md、docs/report-archaeology/PROJECT_REPORT_FACT_BASELINE.md；建议容器/网络/卷拓扑与 down/up 前后核对表。

# 第九章 系统测试、数据库验证与性能分析

本章每个结论要有测试对象、输入、预期、实际结果、证据日期和适用范围。历史 V1 隔离库的负例、8 月 30 日真实故障、Stage H 并发场景、Stage G EXPLAIN、9 月 Docker 恢复不能混为“同一天全量通过”。页面截图只证明表面结果，约束和事务需要 SQL/日志旁证。

## 9.1 数据库结构与负向约束

先核对实际主外键、View 与历史版本，再选会伤害业务的非法写入：不存在房产的账单、重复房产账期、非法月份、错误引用、负读数等。负向测试的意义是数据库在绕过 UI 时仍拒绝坏数据。测试后应保持隔离或回滚，不能污染当前演示库。

### 9.1.1 V1 的 16 个负例与 14 个 seed 断言

database/tests/03_constraint_negative_cases.sql 编号 NEG-01～16，database/tests/05_seed_assertions.sql 有 A01～14；database/evidence/VERIFICATION_SUMMARY.txt 记载预期失败与断言 PASS。正文精选 4～6 个最能对应第五章的 PK/FK/UK/CHECK 案例，保留编号、错误码和失败原因；注明它们只验证当时 V1 结构，不是最终库 V1–V10 的全量回归。

证据与图：上述脚本和 database/evidence/07_negative_cases.txt、09_seed_assertions.txt；建议“非法数据→数据库错误→约束→业务后果”表。

### 9.1.2 最终 Schema 的视图与约束核验

针对最终快照，检查两个 Final View 定义、已记录的 10 条 Flyway history、ownership/residence 等现行约束与代表性业务行。特别验证 V3 产权约束不能冒用为最终库规则。Stage 1 恢复日志已做历史核对，正式写作时若能安全取得新的只读 SHOW CREATE TABLE/VIEW 和 INFORMATION_SCHEMA 输出，应作为最终证据；本稿不生成新的在线结果。

证据与图：2026-09-01 备份末尾 Final view structure、docs/DOCKER_STAGE1_RUNBOOK.md；建议最终 ownership 的 SHOW CREATE 样张与 View 底表依赖图。

## 9.2 事务、一致性与并发

“加了 @Transactional”不是结论，必须解释事务边界覆盖哪些 SQL，失败后哪些记录应回滚，哪些独立请求已经成功。并发案例还需区分 version 条件更新、唯一约束、FOR UPDATE 与服务校验的不同作用，不写成万能并发安全。

### 9.2.1 2026-08-30 的 CHECK 失败与回滚

已有真实 run 记录：先完成独立的创建人员请求，之后入住请求在某个数据库 CHECK 处报 3819，相关事务内部写入回滚。正文应按时间列出“HTTP 请求—SQL 失败—事务处理—最终数据库状态”，突出独立的前一个 HTTP 200 不在后一个事务边界内。不能简单宣称“整个用户操作一切都撤销”，也不把一次错误写成所有入住场景都会失败。

证据与图：logs/run-20260830-163409-707-p34768/access.log、backend.log，docs/PPT_TRANSACTION_ROLLBACK_EVIDENCE.md；建议日志时间线和回滚前后核对图。

### 9.2.2 双人入住冲突与车证审核锁

Stage H 文档记录两名物业员工竞争同一房产：一方 200，另一方 409；随后搬出使状态恢复且 version 增至 2，当前居住关系结束。车证审核的 FOR UPDATE 锁住人和房产，重查资格，条件更新 PENDING 后写许可证。两者分别说明乐观条件冲突与显式行锁的用法，但不能因一次测试就宣称所有可能交错顺序都已形式化验证；pending 申请防重仍依赖前置检查，有边界。

证据与图：docs/STAGE_H_IMPLEMENTATION_REPORT.md、ResidenceManagementMapper.xml、ParkingPermitMapper.xml；建议 200/409 并发时序与审核事务步骤图。

## 9.3 权限和统计正确性

对权限的测试要从可读行数和直接访问返回码两方面展开；对财务统计则要抽样回到原始 bill_item/payment 手算，解释 LEFT JOIN 如何保留未付款账单。性能之前先证明 SQL 的业务结果正确，否则更快地返回错误数字没有价值。

### 9.3.1 社区 Scope 的正反例

Stage H-R1 的 staff_scope_a 与 staff_scope_c 见 1/2 社区，房产总数为 72/168；居民直连旧资产接口返回 403。应将 account_community_scope 的有效行、接口请求身份、SQL IN 参数及页面总数放在同一表中，明确“拒绝跨域”与“多社区并集”都经过验证；日志截图须遮掉敏感账号凭据。

证据与图：docs/STAGE_H_R1_IDENTITY_SCOPE_SECURITY_REPAIR_REPORT.md、AccessControlMapper.xml、PropertyMapper.xml；建议范围对照表和一段脱敏 Flow 日志。

### 9.3.2 财务手算与无记录语义

选一张有账单项、有部分或零支付的账单：应收=SUM(bill_item.amount)，实收=SUM(status=SUCCESS 的 payment.amount)，欠费=MAX(应收－实收,0)；再核对 v_bill_financial_summary 与月度汇总 View，说明 COALESCE、LEFT JOIN 和 NULLIF 的作用。未出账月份没有 View 行，年度图用 null 而非 0；本节应给原表行、View 行和页面图形三层对照。

证据与图：2026-09-01 备份的两个 Final View、stage1/StatisticsMapper.xml、AnnualStatisticsView.vue；建议一张手算表与“空缺/真零”图例。

## 9.4 数据规模、索引与恢复

核心表的数量让查询计划具有观察意义，但不等于大数据基准。报告应先说明数据之间的关联和生成规律，再给索引计划，最后给恢复与短负载场景的适用范围。对性能证据标日期、SQL、行数、工具和环境。

### 9.4.1 演示数据的结构真实性与边界

2026-09-01 快照的 528 房×3 表约等于 1584 表计，528 房×7 期等于 3696 账单，1584 表×10 个读数等于 15840 读数；这是由 FK 和业务关系连接的演示数据，不是无结构随机堆行。上述等式也提示数据具有规则化生成特征，不能说它是自然物业生产分布；完整 528 房批量生成脚本仍未找到。最终合同、车位、部分地图边界表为 0，应与初始 seed 测试的历史样例分开。

证据与图：docs/PPT_FINAL_STATE_AUDIT.md、2026-09-01 备份、database/sample-data/、docs/report-archaeology/REPORT_SECOND_PASS_HIGHLIGHT_AUDIT.md；建议表间数量/外键关系图。

### 9.4.2 代表性索引的执行计划

用 Stage G SQL-03/04 对比账期状态索引前后的 bill 访问路径：扫描约 603 行过滤到 11 行变为索引直接定位 11 行，再经 bill_item 主键查找。补充 account_community_scope PK 的账号前缀查询、meter_reading(meter_id,reading_at) 的每表时间记录定位，但只对有实际 EXPLAIN 证据者给耗时。Stage G 曾统计所审 20 张核心表有 69 个索引对象；该数字不是全库索引总量，更不能将 INFORMATION_SCHEMA 的“索引列记录数”误称索引对象个数。

证据与图：docs/STAGE_G_SQL_PERFORMANCE_AUDIT.md、docs/STAGE_G_INDEX_AUDIT.md、V6；建议一张 Before/After 执行计划，附 SQL 条件和测量环境。

### 9.4.3 恢复、压力场景与可观测追踪

Docker Stage 1 的 down/up 后表、View、中文与 MinIO 对象仍在，是持久化结果；Stage 2 历史短时负载在约 4.1 秒、50 虚拟用户场景下系统请求 1016/1016、数据库场景 945/945 成功，只能说明该场景通过，不能推出生产容量。可观测日志能从 HTTP→Controller→Service→Mapper→SQL→耗时解释某页面数值来源，但需按 Run Session 正确归类，不能混入前次进程的日志。

证据与图：docs/DOCKER_STAGE1_RUNBOOK.md、DOCKER_STAGE2_RUNBOOK.md、DOCKER_STAGE2_5_OBSERVABILITY_RUNBOOK.md、docs/STAGE2_6_RUN_CLASSIFICATION_REVIEW.md、logs/run-20260902-172714-manual-deploy-backend/tests/；建议“恢复核对表＋一条脱敏 SQL Flow”。

# 第十章 总结、设计反思与后续改进

结论围绕数据库课程知识如何在同一真实系统中贯通，而不是列一串框架版本号。要把“做到了什么”和“尚未证实什么”并排写，避免夸大；本文策划稿中暴露的迁移来源缺口、跨存储一致性和深分页边界，正可成为有价值的工程反思。

## 10.1 数据库设计成果

本项目的主线是从空间层级、人房时态关系、收费事实和社区权限需求中抽取实体与联系，再用键/约束防错、View 和 XML SQL 组织查询、事务与锁维护状态、索引改善访问路径，最后用备份和日志检查这些设计在运行中是否成立。总结不复述章节目录，而选 3～4 个跨章节闭环作为证据。

### 10.1.1 从现实规则到可验证关系模式

可回顾“人≠业主≠住户”促成 Ownership/Residence 分表、“同账单多项目且可部分支付”促成 Bill/BillItem/Payment 分离、“一个员工可管理多社区”促成 account_community_scope 与 Mapper IN。它们既有 ER/范式依据，又能在约束、页面指标和测试里回查，体现数据库设计不是事后给已有页面画图。

证据与图：第 3～7 章核心图与路径；结论页可用三行“现实问题→关系结构→验证证据”表，不新增未经证实的成果。

## 10.2 对长期演进与系统边界的认识

数据库中有真实数据后，随意清库重建会丢失人员、账单与文件关联；Flyway、完整备份、对象对账和数据卷各解决生命周期的不同环节。然而最终结构来源缺口显示版本管理不是“引入工具即完成”，关键仍是所有变更进入受审计的迁移链并持续验证。

### 10.2.1 已发现的问题应成为改进路径

后续优先查证 agency_staff、notification、两个 View 和当前态生成列的原始 DDL，核对最终 Schema 与 V1–V10 的差异，建立只读结构 diff 与迁移评审；再考虑深页查询的游标方案、MySQL/MinIO 孤儿对象补偿、车证 NEW_ENERGY 前后端/数据库类型契约、pending 申请数据库级防重以及真实数据库 GRANT 分权。这里列为后续事项，不把尚未修复的问题写成已完成的 Stage。

证据与图：第二轮审计 §7、FloorPlanService.java、V1 vehicle CHECK、ParkingPermitServiceImpl.java；建议“已证事实／待追来源／拟改进”三栏。

## 10.3 答辩与正式成稿的证据纪律

最终正文应做到每个重要数字附数据日期和来源；每张图说明来自真实表/View 还是前端展示补齐；每个“性能提升”绑定 SQL 与 EXPLAIN；每个故障明确是原始日志还是阶段报告记录。若在正式写作时发现策划稿与在线库不同，以新核验记录更新正文，不直接改写历史证据。

### 10.3.1 不把技术名词替代数据库论证

Redis、AMap、Nginx、Docker、JWT 和 Vue 只在解释数据组织、权限、持久化或可验证性时出现。结尾要承认：尚无业务 Redis 缓存命中与加速的证据；地图未用 MySQL 空间索引；MySQL+MinIO 无跨系统原子事务；V1–V10 单独重建最终库未获证实；合同、车位和线上收款不能凭表名宣称完整实现。主动说明边界反而使已有的关系建模、约束、View、Scope 和恢复案例更可信。

证据与图：本稿各章的证据表和两轮审计缺口；建议结尾一页“核心数据库贡献／支撑工程／未完成边界”。

---

## 正式写作前的素材清单与核验顺序

1. **先锁证据版本。** 保存 2026-09-01 SQL 最终 DDL 与两张 Final View、V1–V10、Flyway history、当时 Stage 1 恢复记录；不要拿 mysqldump 中部临时占位 View 当定义。
2. **按章节抽最少必要图。** 第 3 章全局概念 ER＋3 张子图；第 4 章 FD/关系模式与精选数据字典；第 5 章约束反例、最终产权差异、索引计划、财务 View；第 6 章账号 Scope 对照；第 7 章 3～4 条页面→SQL 链；第 8 章迁移和恢复矩阵；第 9 章失败时间线与测试表。避免一页塞数十张截图。
3. **优先核对易错口径。** 最终产权规则、通知/中介员工/View 初建来源、V8 示例 DML、最终合同/车位等空表、账单级付款无法分摊到项目、Redis 与地图非数据库核心、字段 32→64 缺原证据。
4. **再扩成 3.6 万字。** 扩写顺序可先写第 3～6 章的数据库事实，再写第 7～9 章的调用与验证，最后写第 1～2、10 章。每增加一项案例，都回到“业务问题→关系设计→SQL/约束→验证→边界”的同一模板；不要用泛泛的框架介绍填满篇幅。

本稿的路径和数量是写作计划，不代替正式提交前的在线只读复核；本轮没有修改任何项目代码、数据库、迁移、日志或既有文档。
