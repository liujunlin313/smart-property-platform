# 项目事实基线

考古日期：2026-09-24。以 PUBLIC_DEMO 现有源码和 2026-09-01 SQL 备份为准。只读审计，不代表当天在线数据库复核。

## 证据口径与资产地图

| 部分 | 位置 | 用途 |
|---|---|---|
| 项目/设计 | `README.md`、`START_HERE.md`、`PRODUCT.md`、`DESIGN.md` | 目标与设计说明，须用源码校验 |
| 数据库 | `database/migrations/V1__init_schema.sql` 至 `V10__layout_template_floor_plan_variants.sql`，`database/sample-data/` | DDL 演进与样例数据 |
| 数据库验证 | `database/tests/01_schema_check.sql` 至 `05_seed_assertions.sql`；`database/evidence/`、`database/evidence-phase2a/` | 结构、正反例及阶段性测试输出 |
| Spring Boot | `backend/src/main/java/com/smartproperty/platform/`、`backend/src/main/resources/application.yml` | Controller、Service、Mapper 接口、配置；Java 21 |
| MyBatis | `backend/src/main/resources/mapper/` | 实际 SQL、动态条件和分页 |
| Vue | `frontend/src/views/`、`frontend/src/api/`、`frontend/src/router/index.js` | 页面、HTTP 契约与角色路由；Vue 3、Vite、Element Plus、ECharts |
| Docker | `compose.yaml`、`backend/Dockerfile`、`frontend/Dockerfile`、`frontend/nginx.conf` | MySQL、Redis、MinIO、backend、web 五服务，两个持久化 named volume |
| 观测/验证 | `deploy/`、`scripts/`、`logs/`、`docs/DOCKER_STAGE2_RUNBOOK.md` | 既有部署和测试记录；本轮未运行 |
| ER | `docs/ppt_assets/global_er_concept.html`、`docs/ppt-er/01_global_core_er.html` 至 `06_resident_service_notification_er.html` | 全局概念图与业务分图 |
| 结构备份 | `C:/Projects/数据库课程设计/deployment_backups/community_db_before_public_demo_20260901.sql` | 2026-09-01 的实际表、VIEW 最终 DDL 与 Flyway 历史 |

当前 Docker 入口为浏览器→Nginx web→Spring Boot backend→MySQL/Redis/MinIO；`compose.yaml` 定义内部 DNS `mysql:3306`、`redis:6379`、`minio:9000`。密码、JWT、对象存储凭据来自被忽略的 `.env.docker`，本报告不抄写值。`application.yml` 默认 `APP_FLYWAY_ENABLED:false`，Compose 也传 `false`；迁移文件存在不等于每次启动都执行。

## 数字：过去与现在不能混用

| 口径 | 已核数字 | 证据及限制 |
|---|---:|---|
| 2026-09-01 dump 中 `CREATE TABLE` | 47 | 含 `flyway_schema_history`；故主要业务/支撑表 46 |
| VIEW | 2 | `v_bill_financial_summary` 与 `v_monthly_collection_summary`；见 dump 末尾 Final view structure，勿引用中部临时占位结构 |
| 本目录 Flyway migration | V1–V10，10 版 | 迁移文件和备份中的 `flyway_schema_history`；但 `agency_staff`、`notification` 两表及两个 VIEW 的原始建表/建视图未见于这 10 个文件 |
| V1 核心表 | 25 | `V1__init_schema.sql` 25 条建表，不是当前表总数 |
| V1 负例、seed 断言 | 16、14 | `database/tests/03_constraint_negative_cases.sql` 的 NEG-01～16、`05_seed_assertions.sql` 的 A01～14；`database/evidence/VERIFICATION_SUMMARY.txt` 记载预期失败 16、14 PASS；仅代表历史 V1 验证 |
| 2026-08-31 数据量 | 4 小区、14 楼、528 房、1,584 表计、15,840 读数、3,696 账单、18,273 明细、3,324 缴费 | `docs/PPT_DATABASE_PERFORMANCE_AUDIT.md` 历史精确计数；本轮没有查在线库 |
| 2026-09-02 历史负载场景 | 系统 1,016/1,016 成功，数据库场景 945/945 成功 | `logs/run-20260902-172714-manual-deploy-backend/tests/` 两份 JSON；50 虚拟用户、约 4.1 秒，仅说明该场景，不外推长期容量 |

## 46 张主要表的业务域

| 业务域 | 实际表 | 关系与责任 |
|---|---|---|
| 空间资产/户型（7） | `community`、`building`、`building_unit`、`property_unit`、`layout_template`、`layout_template_floor_plan`、`layout_template_floor_plan_variant` | 小区→楼→单元→房，模板及图的两级复用 |
| 人员/权属/居住（3） | `person`、`ownership`、`residence` | 人与房产的拥有和居住是两种可带时间的关系 |
| 租赁/中介（6） | `agency`、`agency_staff`、`authorization`、`rental_listing`、`lease_contract`、`contract_participation` | 三元授权、挂牌、合同与参与人 |
| 收费（5） | `fee_type`、`fee_standard`、`bill`、`bill_item`、`payment` | 收费类别、版本化标准、账单、项目与实收事实 |
| 仪表（2） | `meter`、`meter_reading` | 设备与历史读数 |
| 身份/权限（8） | `app_account`、`app_role`、`app_permission`、`account_global_role`、`account_agency_role`、`role_permission`、`account_community_scope`、`refresh_token_session` | 账号角色、权限、作用域、会话 |
| 通知/报事（3） | `notification`、`notification_recipient`、`resident_incident` | 通知与收件人 N:M 的实体化，居民报事 |
| 停车（6） | `parking_space`、`parking_right`、`vehicle`、`residence_parking_eligibility`、`parking_permit_application`、`parking_permit` | 车位权、车、居住资格、申请、已发证分离 |
| 文件（2） | `file_object`、`property_floor_plan` | MySQL 元数据与 MinIO 二进制对象分离 |
| 设施/审计/空间（4） | `facility_type`、`facility_placement`、`asset_change_audit`、`spatial_boundary` | 房内二维设施、资产审计、地图范围 |

`Room`、`Occupancy` 不是独立表（`database/tests/05_seed_assertions.sql` A14）。`property_unit.occupancy_status` 是当前占用状态，`residence` 保存人房入住事实与历史；不要把二者混为一个实体。

## 系统现状与可写入报告的真实评价

当前前端同时保留 `/api/v1` 资产工作台、`/api/admin` 管理界面和 `/api/resident/me` 居民本人接口；不能把两个房产列表接口合成一条虚构链路。`frontend/src/api/http.js` 为统一认证 HTTP 客户端，处理 JSON envelope 与 401 刷新。管理页面覆盖房产、居民、租赁、账务、统计、Dashboard、地图、通知、入住和车证等；页面存在不等于所有业务均可写。历史审计 `docs/PPT_DATABASE_PERFORMANCE_AUDIT.md` 明确：当前未见线上账单批量生成及真实收款入账 Service/Mapper 写入口，相关数据用于读取与统计。

可作为亮点：①时间化 Ownership/Residence 而非把“人=业主=住户”；②复合唯一、FK、CHECK 与 16 个负向验证；③多小区角色+权限+作用域+Mapper SQL 过滤；④两层账务 VIEW；⑤资产乐观版本更新；⑥通知/车证/入住多表事务；⑦MinIO 对象与 MySQL 元数据分离；⑧Flyway V1–V10 和已有 SQL 链路观测。前六项贴合数据库课程，第七、八项兼有工程价值。需如实写出不足：VIEW 与两张历史表的结构来源未形成完整迁移闭环；V1 隔离库测试不能冒充现今 Docker 全量回归；压力场景不等于所有业务容量；Redis/地图/日志不是关系模型创新。
