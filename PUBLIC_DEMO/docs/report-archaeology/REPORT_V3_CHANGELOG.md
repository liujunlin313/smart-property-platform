# 数据库课程设计报告 V3 增量修改记录

修改日期：2026-10-01。

基线为 PUBLIC_DEMO 项目 `docs/report-archaeology/REPORT_FULL_STRUCTURE_DRAFT_V2.md`。交付为同目录新增 `REPORT_FULL_STRUCTURE_DRAFT_V3.md` 与本文件；V2 Markdown 与已生成的 V2 Word 均保留。此次工作仅阅读已有材料、修改报告文稿，没有运行服务、数据库写入、迁移、构建或业务测试，没有修改源码、Docker 配置、日志或同步远程仓库。

## 1. 增量内容与位置

| 新增或扩写位置 | 内容 | 写作作用 |
|---|---|---|
| 1.2 总体设计思想与开发路线 | 1.2.1 事实与约束驱动的总体路线；1.2.2 问题—设计—实现—验证演进表；1.2.3 双环境职责 | 从数据库事实出发说明系统怎样逐步形成，不以技术栈罗列替代方法论 |
| 第一章原 1.2、1.3 | 顺延为 1.3、1.4，子标题同步编号 | 在第一章前部容纳设计路线，原段落内容保留 |
| 7.5.2 地图坐标落库与表计时序 | 一段经纬度、来源与核验属性、缺失坐标更新及 MapScene 查询的说明 | 说明位置事实进入 API 与地图；不把列存在等同于每条数据已人工核验 |
| 8.4 从稳定开发环境到 PUBLIC_DEMO 的部署演进 | 冻结/副本、备份/恢复、三数据服务到应用构建、Nginx 交付、局域网、临时公网边界 | 明确环境改造如何保护既有关系数据并形成网站入口 |
| 8.5 工程版本机制与四种生命周期 | 源码、镜像、容器、数据；Git、Flyway、Compose、镜像、volume、backup 职责表 | 区分版本机制、结构演进、在线持久化与历史恢复 |
| 9.5 部署环境验收 | 健康与连接、关键记录/View/中文/对象、HTTP 与人工交互、持久化、终端访问证据 | 用核对链证明运行成果，并准确限定哪些场景有执行证据 |
| 文末稿件核验说明 | 更新版本范围、编号调整及新增缺口 | 使读者能区分原待核验项与新增部署证据边界 |

对应本轮要求的六个回答：整体思想在 **1.2**；双环境在 **1.2.3 与 8.4.1**；Docker 在 **8.4.3—8.4.4**；局域网/网站演示在 **8.4.4—8.4.6 与 9.5.5**；Git/Flyway/Docker 的区分在 **8.5**；部署验收在 **9.5**。

## 2. 保留的数据库内容

十章总结构保留。通过分章原文比较，第二至第六章及第十章正文与 V2 完全一致；包括需求、ER、1NF/2NF/3NF、函数依赖、Ownership/Residence、账务拆表、PK/FK/UNIQUE/CHECK、生成列、索引、EXPLAIN、两个财务 View、LEFT JOIN、DataScope 及权限正反例。

第七章仅在 7.5.2 加一段地图持久化说明，其余 MyBatis、分页、统计、入住事务、通知、车证和文件链原文保留。第八章原 8.1—8.3、V1—V10 清单、迁移来源缺口、产权兼容、备份与 down/up 原文保留，新增 8.4/8.5。第九章原 9.1—9.4、负向 SQL、CHECK 回滚、200/409 并发、scope、财务手算、规模与性能原文保留，新增 9.5。未用部署内容替换已有数据库论证。

V2 的迁移来源缺口、字段长度口述待核验、全量数据发生器待核验均保留。新增演进表特别说明：Ownership/Residence 与 Bill/BillItem/Payment 在可见 V1 中已经拆分，不能编造“后期从一张大表拆出”的故障及版本历史。

## 3. 本轮核对的证据

以下工程相对路径均以 `C:\Projects\数据库课程设计\多小区智慧物业管理系统_PUBLIC_DEMO` 为根。表内只说明已有记录，本轮未重新执行其验收。

| 材料 | 支撑内容与证据类型 |
|---|---|
| `docs/PUBLIC_DEPLOYMENT_PRECHECK.md`，尤其 §19—20 | 稳定版保留、独立副本、备份与迁移路线；预审记录当时未发现 Git 仓库。路线是计划，不单独用来证明全部执行成功 |
| `docs/DOCKER_STAGE1_RUNBOOK.md` 的 Executed record/result | 2026-09-01 三数据服务、独立卷/网络、SQL 恢复、中文、View/约束、12 对象、down/up 与 PONG 的历史执行记录 |
| `docs/DOCKER_STAGE2_RUNBOOK.md` | 五容器架构、内部 DNS、Nginx 同源入口、Flyway 初次禁用、组件局部更新及临时 LAN 约定；操作说明与执行结果分开 |
| `compose.yaml` | 五服务、端口、内部地址、依赖、健康检查、两个 named volume；以静态配置证明运行约定，不由此推断所有运行结果 |
| `backend/Dockerfile`、`frontend/Dockerfile`、`frontend/nginx.conf.template` | Java/Vue 构建与运行分层、production 静态交付、/api/ 代理和 history 回退 |
| `docs/STAGE2_6_CHANGE_REPORT.md` | 五容器 healthy、编译/build、Nginx 配置、健康请求、401、58/58 与 61/61 smoke 的历史验收；文档明确本阶段未增加 LAN/Tunnel 暴露 |
| `logs/run-20260902-164511-manual-deploy-web/nginx/access/access.jsonl` | 局域网来源 LAN_HOST 的早期登录 403；Android 15 的 heartbeat 200 及登录失败请求 |
| `logs/run-20260902-171602-manual-deploy-backend/nginx/access/access.jsonl` | 同一入口 host 后续出现登录 200；不能直接对应为移动终端完整登录成功 |
| `logs/run-20260902-172714-manual-deploy-backend/nginx/access/access.jsonl` | LAN_HOST 来源的登录与旧房产 API 200；证明该入口的真实请求，不仅是配置中列出 IP |
| 上述 172714 会话 `tests/system-load-20260902-223609/system-load-report.json` | localhost:18080、50 虚拟用户、1016/1016、登录/Dashboard/小区/楼栋/房产/身份动作 |
| 上述 172714 会话 `tests/database-load-20260902-223806/database-load-report.json` | 945/945；protected tables 前后数量相同；person 当时为 607，不与恢复时 606 混成同一快照 |
| `database/migrations/V3__phase2a_security_assets_and_files.sql`；`backend/src/main/resources/mapper/map/MapLocationMapper.xml`、`MapSceneMapper.xml` | 空间属性及缺失坐标更新/场景查询链；不声称使用 MySQL GIS |
| `docs/report-archaeology/PROJECT_REPORT_FACT_BASELINE.md`、前两轮考古与证据索引 | 原数据库规模、迁移来源缺口、历史运行和源码证据纪律 |

独立备份来源为同级 `C:\Projects\数据库课程设计\deployment_backups` 下的 `community_db_before_public_demo_20260901.sql`、`minio_data_before_public_demo_20260901`、`runtime_config_before_public_demo_20260901`。本轮确认其目录存在并沿用 Stage 1 恢复记录，没有读取或公开凭据值。

## 4. 尚缺证据或需要限定的部署事实

1. **移动终端完整热点验收。**已找到 Android 心跳成功和早期登录失败，以及局域网 host 登录与房产读取成功；未找到能够把手机/Pad、同一热点、成功登录与全部业务页面串成同次完整验收的记录。正文分别说明现存日志与用户现场记忆。
2. **校园网客户端隔离。**可解释请求无法到达服务器的一类网络风险，但尚不能确定某次项目失败由此造成。Nginx 已记录的 403 表示请求到达入口，不能再把同一失败直接归因于终端隔离。
3. **早期 403 的唯一根因。**后续 origin 配置包含局域网 host，且后续请求成功，但缺少当次完整配置与原始响应体，不能断言就是某一次 CORS 修复或特定代码提交解决。
4. **Quick Tunnel。**有历史需求、预审路线及启动入口，未定位完整 HTTPS、登录刷新、文件、地图的公网成功闭环。只写临时演示准备与待验收，不写长期公网生产部署。
5. **Stage 2 五容器完整 down/up。**Stage 1 三数据服务恢复后的持久化执行证据充分；Stage 2 有约定、五服务 healthy 与 HTTP 记录，未定位同样详细的五容器重建后全业务再验收记录。未把 Runbook 命令当执行结果。
6. **Docker 环境全角色与所有浏览器流程。**现有 Windows Stage H/H-R1 角色证据及 Docker HTTP 场景各自有效；不能合并成 Docker 三角色、入住、账单、所有图纸和地图均人工验收通过。
7. **最终 Schema 来源。**agency_staff、notification、两个 View 及部分最终规则的原始变更仍未完整定位；恢复成功不意味着 V1—V10 可独立无差异重建最终库。
8. **Git/GitHub 与数据保护。**不声称已上传 GitHub、不补造 Git 历史；volume 与 backup 分开说明，未称 Docker 自动保护业务数据，也未声称异地灾备或高可用。

## 5. 图号与后续制作

| 图号 | 新增图注内容 | 可复用材料与制作边界 |
|---|---|---|
| 图 1-2 | 系统总体设计与开发路线 | 正文路线及反馈验证箭头；不是技术栈海报 |
| 图 1-3 | 项目演进时间线 | 复用图 8-1 V1—V10 中段与阶段材料；不虚构早期合表版本 |
| 图 8-2 | 双环境关系 | 部署预审与 Stage 1 基线/备份/独立实例；仅初始化箭头，无自动同步 |
| 图 8-3 | Docker 五容器拓扑 | 复用 Stage 1/2 Runbook，明确内部端口、入口和两个数据卷 |
| 图 8-4 | 局域网访问 | WLAN/热点、电脑网卡 IP、18080 入口、后端数据服务；与实测覆盖区分 |
| 图 8-5 | 版本机制与生命周期 | 源码—镜像—容器与 Schema/数据两条路径；volume 不画成 backup |
| 图 9-2 | 部署验收流程 | 各层证据与待补场景分别标识，不用一个 PASS 覆盖所有步骤 |

本轮写入正式图号、图注及图中元素说明，未生成新图文件。原有 ER、财务 View、索引、作用域、迁移及事务图注保留；图 8-1 可作为演进时间轴的复用基础。

## 6. 字数与稿件检查

为避免中英文标识、SQL 和 Markdown 对“字数”产生歧义，使用两种相同口径比较，均包含摘要、表格、图注、参考文献/附录占位及稿件说明，不含本 changelog。

| 统计口径 | V2 | V3 | 增加 |
|---|---:|---:|---:|
| 汉字数（CJK Unified Ideographs） | 27,727 | 36,851 | 9,124，约 32.91% |
| 非空白字符数（含英文字母、数字及标点） | 43,861 | 59,203 | 15,342 |

V3 十章保留；编号无重复；七张新增图注各出现一次。第二至六章、十章逐章比较完全相同，第七章追加段落之外原文保留，第八章 8.1—8.3 与第九章 9.1—9.4 原文保留。V2 基线 SHA-256 为 `C6211488730E6E1411B91A7A08FAFA8D7BC1D077A7A3CE16A43AEE1773387B2A`，交付前再次核对以确认没有覆盖。

此次交付为 Markdown V3 与修改记录；既有 48 页 Word 对应 V2，不能将其目录页码直接当成 V3 页码。
