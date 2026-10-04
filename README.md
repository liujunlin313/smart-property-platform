# 多小区智慧物业管理系统

本项目是数据库课程设计形成的 Java/Web 系统。关系数据库设计是核心，Spring Boot、MyBatis 与 Vue 将数据模型用于实际业务，Docker 版本用于容器化演示与恢复验证。

如果希望阅读数据库与本地开发工程，查看 **MERGED_V1**；如果希望查看 Docker 化网站演示方案，查看 **PUBLIC_DEMO**。两个目录是同一系统面向不同运行环境保留的成果版本，并非自动同步的实例，也不是简单的新旧替换关系。

## 数据库与业务设计

空间资产按 Community → Building → BuildingUnit → PropertyUnit 组织；人员、产权和居住事实分别存储。Bill、BillItem 与 Payment 保留应收及实收的不同业务事实，财务 View 支持聚合查询。管理员的小区授权通过 account_community_scope 与 Service/DataScope/Mapper 链传递。SQL 明确保存在 MyBatis Mapper XML，事务、条件更新和 version 支持业务一致性。MySQL 保存文件元数据，MinIO 保存对象内容。

Flyway V1—V10 保留数据库演进记录。**迁移链与最终历史数据库存在已知结构来源缺口**，不保证仅执行 migrations 就能重现最终演示快照。请阅读 [数据库初始化边界](docs/DATABASE_INITIALIZATION.md)。

## 目录

```text
MERGED_V1/       Windows 本地稳定开发版本
PUBLIC_DEMO/     Docker 容器化演示版本，仍使用 frontend/ 目录
docs/           版本关系、安全说明、初始化边界与验证记录
GITHUB_PUBLICATION_AUDIT.md
ORIGINAL_FILES_UNTOUCHED_CHECK.md
```

每个版本保留 backend、frontend、database、deploy 及阶段文档。未抽取共享代码，未改变 Maven/Vue 构建结构。

## 技术版本与依据

| 组件 | 实际声明/锁定 | 依据 |
| --- | --- | --- |
| Java | 21 | backend/pom.xml；Docker JRE 21 |
| Spring Boot | 3.5.16 | backend/pom.xml |
| MyBatis Spring Boot | 3.0.5 | backend/pom.xml |
| MinIO Java SDK | 8.5.7 | backend/pom.xml |
| Vue / Vite | 3.5.41 / 7.3.6 | 两版 frontend/package-lock.json |
| Element Plus / ECharts / Axios | 2.14.4 / 6.1.0 / 1.19.0 | package-lock.json；package.json 声明的是版本范围 |
| Docker Maven / Node | 3.9.9 / 24-alpine | PUBLIC_DEMO 的两个 Dockerfile |
| MySQL / Redis | 8.0.43 / 7.4-alpine | PUBLIC_DEMO/compose.yaml |
| MinIO Server | RELEASE.2025-07-23T15-54-02Z | PUBLIC_DEMO/compose.yaml |
| Nginx | 1.27-alpine | PUBLIC_DEMO/frontend/Dockerfile |

Flyway 由 Spring Boot 依赖管理解析；Docker Compose 未在工程中固定工具版本。本地脚本保留历史工具路径并提供 PATH 回退，不应把历史路径视为其他开发者必须使用的安装位置。

## Windows 本地开发

安装 JDK 21、Maven、Node，并自行准备 MySQL 的 community_db、Redis 和 MinIO。配置自己的数据库账号及权限。复制示例并填写本地值：

```powershell
cd MERGED_V1
Copy-Item deploy/local.env.example.properties deploy/local.env.properties
Copy-Item frontend/.env.example frontend/.env.local
```

实际配置不要提交。JWT Base64 密钥、数据库密码、MinIO 凭据及两类高德 Key 应分别配置；JS Key 必须与对应 securityJsCode 配对。Web Service 私钥由后端用于动态签名。

后端可从 IDEA 运行 SmartPropertyApplication，或运行 `deploy/start-backend-dev.ps1`。前端单独运行：

```powershell
cd frontend
npm ci
npm run dev
```

前端地址为 `http://127.0.0.1:5173`，Vite 将 API 请求代理到 `127.0.0.1:8080`。WebStorm 等 IDE 应运行 package.json 的 dev 脚本，不应把 src/main.js 当成 Node 程序直接执行。本地启动器可能准备 Redis/MinIO 并管理其 PID，请先阅读 deploy/README.md。数据库初始化未完成时不能宣称登录或业务页面可用。

## Docker 演示

进入 PUBLIC_DEMO，将 `.env.docker.example` 复制为 `.env.docker` 并填写自己的值。日志目录变量包含 `PUBLIC_DEMO_LOG_ROOT` 和 `PUBLIC_DEMO_ACTIVE_RUN_LOG_ROOT`。示例中的占位凭据仅便于解释变量，不能用于实际运行。

```powershell
cd PUBLIC_DEMO
Copy-Item .env.docker.example .env.docker
docker compose --env-file .env.docker config --quiet
docker compose --env-file .env.docker build backend
docker compose --env-file .env.docker build web
```

**在准备并核验完整数据库和对象数据后**，才运行 `docker compose --env-file .env.docker up -d`。本仓库不附带原始数据库备份、历史账号、对象存储数据或真实密钥，空卷首次启动不会自动得到完整课程演示系统。

网站入口为 `http://127.0.0.1:18080`；后端调试端口 18081、MySQL 33307、MinIO 19000/19001 默认绑定 loopback。Redis 不暴露主机端口。容器内部使用 mysql:3306、redis:6379、minio:9000 和 backend:8080。调整 LAN 入口时自行填写 `WEB_BIND_ADDRESS`、`APP_ALLOWED_ORIGINS`。

Compose 保留历史固定 named volume/network 名。运行不同目录的 Compose 仍可能指向同一组数据卷，操作前必须核对；勿执行 `down -v`。本仓库是演示成果，不承诺生产高可用或长期公网部署。

## 构建与安全验证

在每个版本的 backend 可运行 `mvn test -Dtest=ChargeNoticeComposerTest,NotificationServiceImplTest`；这组选定测试使用 mock，不连接长期数据库。前端可运行 `npm ci` 后 `npm run build`。其余 SQL 验证、业务 smoke 和 load test 可能写入数据，须在独立授权环境运行。

真实 local.env、.env、日志、运行数据、SQL 备份、截图归档及构建产物已从发布副本排除。合成 SQL 样例与 migration/test 源码保留。历史文档所引用的部分截图和证据未公开，因此相关旧链接可能不存在。详见 [安全说明](docs/SECURITY_NOTE.md) 和 [发布审计](GITHUB_PUBLICATION_AUDIT.md)。尚未选择开源许可证，当前未添加 LICENSE。
