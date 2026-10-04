# Public Deployment Precheck

> 审计日期：2026-09-01  
> 审计方式：源码、配置、脚本、已有运行目录、只读 MySQL 查询和本机 Docker/WSL 命令。  
> 本文不执行 Docker 化、不启动或停止服务、不导入/导出数据，也不修改现有业务文件。

## 0. Executive Summary

当前工程**可以迁移为独立的 Docker 公网演示版**，但不应直接复制当前本地配置后启动。后续工作量评估为 **Medium**：前端 API 已是同源相对路径，MinIO 文件模型也已与 endpoint 解耦；但当前启动入口明确以 Windows 本机开发为目标，默认绑定回环地址，并自动发现本地配置文件、准备本机依赖服务。

必须先解决的 P0 为 4 项：

1. 后端默认 `SERVER_ADDRESS=127.0.0.1`，容器内 Nginx 无法访问后端。
2. `SmartPropertyApplication` 无条件进入 `LocalDevelopmentServices`；若复制了当前 `deploy/local.env.properties`，Docker 会尝试使用 Windows 路径、本机脚本与开发预检。
3. Vue Router 使用 history 模式，未来 Nginx 必须提供 `/index.html` 回退，否则 `/map` 等深链接会 404。
4. 公网 HTTPS 下 refresh cookie 仍默认为非 Secure；公网配置必须显式启用 Secure 并保留转发头处理。

数据库完整逻辑快照恢复到 Docker MySQL **可行且必要**；MinIO bucket/object 的完整复制到 Docker MinIO 也**可行且必要**。现有 `file_object` 保存 bucket 与 object key，而非 MinIO 完整 URL；文件下载由后端代理流式返回浏览器，不会把 Docker 内部 `minio:9000` 暴露给浏览器。

本机 Docker CLI/Compose 均不可用，WSL 状态输出要求安装，因此 Docker Desktop/WSL2 当前**未就绪**。这不是源码阻断，但会阻断后续实际容器验收。

## 1. Current Runtime Topology

| 服务 | 当前运行地址 | 端口 | 谁访问它 | 配置来源 | 是否硬编码 | Docker 后是否需要变化 |
|---|---|---:|---|---|---|---|
| Vue/Vite 开发服务器 | `127.0.0.1` | 5173 | 本机浏览器 | `frontend/package.json:7`、`vite.config.js:7` | 开发硬编码 | 生产改为静态构建，由 Nginx 提供 |
| Spring Boot | `127.0.0.1` | 8080 | Vite 代理/本机浏览器 | `application.yml:2-3`、开发脚本 | 默认回环地址 | 容器内应监听 `0.0.0.0:8080`，仅 Docker 网络访问 |
| MySQL | `127.0.0.1` | 3306 | Spring Boot | `application.yml:16`、本地环境配置 | 默认回环地址 | backend 改用 Docker 服务名 `mysql:3306` |
| Redis | `127.0.0.1` | 6379 | Spring Boot（当前为可选诊断/性能层） | `application.yml:31-33` | 默认回环地址 | backend 改用 `redis:6379` |
| MinIO API | `127.0.0.1` | 9000 | Spring Boot | `application.yml:85`、本地环境配置 | 默认回环地址 | backend 改用 `http://minio:9000` |
| MinIO Console | `127.0.0.1` | 9001 | 本机管理员 | 本地环境配置 | 本机开发约定 | 公网演示默认不暴露；按需仅本机管理 |
| 高德 Web Service | `https://restapi.amap.com` | 443 | Spring Boot | `application.yml:94` | 可配置 | 不改 host；容器需能访问公网 HTTPS |
| 高德 JS API | 高德 HTTPS Loader | 443 | 浏览器 | `frontend/src/map/amapLoader.js:4-21`、Vite 环境变量 | build-time 配置 | 保留 HTTPS；需核验公网域名白名单 |
| cloudflared Quick Tunnel | 尚未安装/配置 | — | 未来连接 Nginx | 无现有生产配置 | — | 后续仅暴露 `web` 容器入口 |

浏览器当前经 Vite 访问前端；`frontend/vite.config.js:9-16` 将 `/api`、`/actuator` 转发至本机后端。生产形态应变成：浏览器 → Cloudflare HTTPS → Nginx `web` → `/api` 反代 backend → MySQL/Redis/MinIO（仅 Docker 内部网络）。

## 2. Port Inventory

| 端口 | 当前用途 | 浏览器应否直接访问 | 公网 Docker 建议 |
|---:|---|---|---|
| 5173 | Vite 开发服务 | 仅开发 | 不使用 |
| 8080 | Spring Boot | 开发可直连 | 不发布宿主端口；Nginx 内网反代 |
| 3306 | MySQL | 否 | 不发布 |
| 6379 | Redis | 否 | 不发布 |
| 9000 | MinIO API | 否 | 不发布 |
| 9001 | MinIO Console | 否 | 默认不发布 |
| 80（未来） | Nginx web 容器 | 是（由 Tunnel 访问） | 仅容器网络；Quick Tunnel 指向该端口 |

## 3. Hardcoded Host / URL Inventory

| 文件与行号 | 当前值/摘要 | 使用者 | 当前能工作原因 | Docker 后处理 |
|---|---|---|---|---|
| `backend/src/main/resources/application.yml:3` | `${SERVER_ADDRESS:127.0.0.1}` | 嵌入式 Tomcat | 本机浏览器与后端同机 | **P0**：生产环境变量设为 `0.0.0.0` |
| `application.yml:16` | JDBC 默认 `127.0.0.1:3306/community_db` | backend → MySQL | MySQL 在同一 Windows 主机 | Docker 配置改为 `mysql:3306` |
| `application.yml:31-32` | Redis `127.0.0.1:6379` | backend → Redis | Redis 在同一 Windows 主机 | Docker 配置改为 `redis:6379` |
| `application.yml:77` | CORS 默认 `http://localhost:5173` | Spring Security/CORS | Vite 开发跨源时适用 | 同源 Nginx 下通常无需 CORS；若保留跨源，显式配置公网 origin |
| `application.yml:85` | MinIO 默认 `http://127.0.0.1:9000` | backend → MinIO | MinIO 在同一 Windows 主机 | Docker 配置改为 `http://minio:9000`，不返回浏览器 |
| `frontend/vite.config.js:11,15` | Vite proxy `http://127.0.0.1:8080` | 仅 Vite 开发代理 | 本机开发 | 不进入生产构建；Nginx 应反代 `backend:8080` |
| `frontend/package.json:7,9` | Vite/preview 绑定 `127.0.0.1` | 开发命令 | 本机开发 | 生产不运行 Vite |
| `deploy/start-backend-dev.ps1:46-48` | `APP_LOCAL_CONFIG` + `SERVER_ADDRESS=127.0.0.1` | Windows 开发启动 | 开发脚本 | 不能作为容器启动命令 |
| `deploy/local.env.properties` | 本机 DB/Redis/MinIO 与 Windows 可执行文件、数据目录 | 本机开发引导 | 本地稳定环境 | **P0**：不得复制为 PUBLIC_DEMO Docker 配置；改为独立 secret/env 文件 |
| `LocalDevelopmentConfiguration.java:23,38,94` | 自动查找/加载 `deploy/local.env.properties` | Java 进程 | 从 IDEA/PowerShell 发现本地配置 | 生产应禁用开发引导，且不要将该文件放入容器 |
| `deploy/local.env.properties` | 包含 `C:\Projects\...`、`C:\Program Files\...` 等路径 | 本机脚本 | Windows 路径存在 | 不可进入 Linux 容器；替换为容器环境变量/volume |

未发现前端业务 API 使用 `http://localhost:8080` 或 `http://127.0.0.1:8080` 的绝对 URL。未发现 `host.docker.internal`、浏览器直连 MinIO URL、或把 Windows 路径作为前端静态资源 URL 的业务实现。

## 4. Frontend Deployment Audit

### API 与路由

- `frontend/src/api/http.js:9` 统一创建 Axios 实例，`baseURL: '/api'` 且 `withCredentials: true`。这是生产同源 Nginx 部署的正确基础。
- 逐项检查前端的 `fetch` 后，`frontend/src/observability/clientMonitor.js:52` 使用的是相对 `/api/v1/dev/client-events`；其他业务请求也经该 Axios 实例或相对路径发出。
- `frontend/src/router/index.js:52` 使用 `createWebHistory()`，没有配置非根路径 base。生产 Nginx 必须使用 `try_files $uri $uri/ /index.html`；否则直接刷新 `/map`、`/login`、居民端路由会返回 404。此项为 **P0**。
- 前端部署根路径假设为 `/`；后续不要把站点放在未知子路径，除非一并配置 router base 与静态资源 base。

### 静态资源与地图

- 构建静态资源由 Vite 产物提供，未发现业务代码硬编码 Windows 文件地址。
- `frontend/src/map/amapLoader.js:4-21` 从 `VITE_AMAP_JS_KEY`、`VITE_AMAP_JS_SECURITY_CODE` 读取前端高德凭据，先设置 `window._AMapSecurityConfig`，再加载 HTTPS 高德 loader。`VITE_*` 是**构建时**注入：PUBLIC_DEMO 需要在前端镜像构建阶段提供自己的本地秘密配置，不能指望容器运行后再改变已构建 JS。

### 户型图/文件访问链路

实际链路为：

`PropertyDrawer.vue:84` → `assets.js:35` 相对 API GET → `FloorPlanController.java:39-48` → `FloorPlanService.java:115-130` 从 MinIO `getObject` → `InputStreamResource` 返回浏览器。

上传链路为：`PropertyDrawer.vue:105` → `assets.js:26-33` multipart POST → `FloorPlanService.java:66-100` 写入 MinIO 与数据库关联。

结论：当前模型是 **C. Spring Boot 代理/流式下载**，不是浏览器直接访问 MinIO，也不是 Presigned URL。Docker backend 使用 `http://minio:9000` 仅限容器内部，前端不会收到内部 hostname；这一点是迁移有利条件。

## 5. Backend Deployment Audit

### 本地开发引导

- `SmartPropertyApplication.java:14` 无条件调用 `LocalDevelopmentServices.start()`。
- `LocalDevelopmentServices.java:20` 仅当发现的配置不存在或 `APP_DEV_BOOTSTRAP=false` 时跳过；当前本地 `deploy/local.env.properties:3` 为 `APP_DEV_BOOTSTRAP=true`。
- `LocalDevelopmentConfiguration.java:23,38` 会读取显式 `APP_LOCAL_CONFIG` 或从当前工作目录/代码位置向上搜索 `deploy/local.env.properties`。
- `DevScriptRunner.java:25` 与 `DevProcessRegistry.java:15` 会把开发进程记录写入 `deploy/runtime`。

这套机制适合当前 Windows 本地开发，但容器中若误带入本地环境文件，会尝试依赖本机开发预检、Windows 工具与项目路径。因此 PUBLIC_DEMO 必须采用独立生产环境变量，并设置 `APP_DEV_BOOTSTRAP=false`。后续可考虑把开发引导限制为明确 local profile；本轮不改。

### 反向代理与健康检查

- `application.yml:4` 已使用 `forward-headers-strategy: framework`，是 Cloudflare HTTPS → Nginx HTTP → backend 的良好基础。
- Spring Security 允许健康检查路径（代码审计确认 `/actuator/health` 可作为 future container healthcheck）。
- 未发现 WebSocket、SSE 或业务长轮询实现；文件下载为普通 HTTP 流式响应。Nginx 未来无需 WebSocket 专项配置，但应测试大文件下载/上传。
- `spring.servlet.multipart.max-file-size=10MB` 与 `max-request-size=12MB`（`application.yml:40-41`）。Nginx future `client_max_body_size` 至少应为 12MB，建议留余量（例如 16MB），否则户型图上传会被 Nginx 提前拒绝。

## 6. MySQL & Flyway Audit

### 当前数据库事实（只读验证）

| 项目 | 结果 |
|---|---|
| Schema | `community_db` |
| MySQL | 8.0.43 |
| 端口 | 3306 |
| 字符集 / Collation | `utf8mb4` / `utf8mb4_0900_ai_ci` |
| JDBC 时区声明 | `Asia/Shanghai`（`application.yml:16`） |
| SQL Mode | `ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION` |
| Base tables | 47 |
| Views | 2：`v_bill_financial_summary`、`v_monthly_collection_summary` |
| 演示数据量 | property_unit 528；person 606；bill 3696；meter 1584；file_object 12 |

### 数据源与迁移

- 数据源读取 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`（`application.yml:16-18`）；真实密码未写入本文。
- Flyway migration 源在 `database/migrations/V1` 至 `V10`，构建时由 `backend/pom.xml` 打包进 classpath `db/migration`。
- 当前 `application.yml:26` 默认 `APP_FLYWAY_ENABLED=false`；本地环境也保持关闭。数据库中的 `flyway_schema_history` 已存在且成功历史到 **V10**。
- 现有 528 房、人员、账单、表计等不是仅靠 V2 reference seed 可完全复建；其来源包括后续 migration、`database/sample-data/01_demo_data.sql` 与当前数据库历史累积。因此 PUBLIC_DEMO 必须恢复**完整一致的数据库快照**，不能改用空库 + migration。
- 两个 View 当前为 `SQL SECURITY DEFINER`，定义者为本机 MySQL root localhost。未来 dump/restore 必须保留 View 定义，并在 Docker MySQL 上验证 DEFINER 与权限兼容性；否则视图查询可能失败。

### 恢复时必须保留

Base tables、全部演示数据、索引、外键、CHECK/约束、AUTO_INCREMENT 当前值、views、view definers/权限、`flyway_schema_history`、schema charset/collation、SQL mode 与业务时区约定。

快照恢复后首启建议仍保持 `APP_FLYWAY_ENABLED=false`，并确认历史表和 schema 一致后再决定是否开启。若 dump 漏掉 `flyway_schema_history`，Flyway 可能把已有表误判为未迁移而造成验证/重复迁移冲突。

## 7. MinIO Audit

| 项目 | 结果 |
|---|---|
| 当前 API endpoint | 本地配置为 `http://127.0.0.1:9000` |
| 当前 Console port | 9001 |
| bucket | `property-assets` |
| DB 关联模型 | `file_object.bucket_name` + `file_object.object_key`，另存元数据 |
| 完整 URL 是否入库 | 未发现 |
| 文件访问模型 | 后端 MinIO streaming/proxy |
| DB 中 file_object 数 | 12 |

`FileObjectMapper.xml:5-10` 持久化 bucket/object key；`FloorPlanService.java:71` 按 `property-units/{id}/...` 生成对象 key；`FloorPlanService.java:126-130` 从配置 endpoint 读取对象。endpoint 不进入 DB，这满足“新 MinIO endpoint 不改变既有 DB 关联”的前提。

`StorageAvailabilityService.java:47-50` 会检查 bucket，不存在时自动创建。它不会替 PUBLIC_DEMO 复制既有对象、bucket policy 或 CORS；因此先恢复 MinIO 数据，再启动 backend 才能保证已有户型图可用。

当前未使用 Presigned URL，也未发现 public bucket 直链。未进行 MinIO object listing，因为当前只读 MinIO CLI/管理配置不可确认；物理对象数量应在后续迁移前以只读 `mc ls`/`mc stat` 核实，不可用 DB 的 12 条记录替代 object store 完整性检查。

## 8. Redis Audit

- 配置来源：`application.yml:31-33`，默认 `127.0.0.1:6379`，密码由 `REDIS_PASSWORD` 环境变量提供（若配置）。
- 源码中确认 Redis 当前用于可用性检查/性能层；未发现 Redis session、JWT blacklist、验证码、分布式锁、限流、登录状态或业务事实的持久化使用。
- 数据库仍是业务事实来源；因此 PUBLIC_DEMO 使用 `redis:6379` 足够，**Redis runtime data 无需复制**。Redis 容器可使用空新 volume 或无持久卷。

## 9. Filesystem / Absolute Path Audit

| 位置 | 结论 | Docker 处理 |
|---|---|---|
| `deploy/local.env.properties` | 含 Windows 项目目录、MySQL/MinIO/Redis executable 路径及本地 runtime 路径 | 不复制到镜像；改用 Docker env 与 named volumes |
| `deploy/*.ps1`、`deploy/scripts/dev/*` | Windows 本地启动、端口检查、PID/日志工具 | 仅开发工具；PUBLIC_DEMO 不调用 |
| `deploy/runtime/` | 当前含 `dev-processes`、`logs`、`redis-data`、smoke/diagnostics | 本地运行状态；不复制为源码/容器运行依据 |
| `backend/target`、`frontend/node_modules`、`frontend/dist` | 构建产物 | 容器应在 Linux build stage 内生成，不复制宿主产物 |
| 户型图业务内容 | MinIO，不依赖本地 uploads 目录 | MinIO named volume + object restore |

未发现业务上传内容写入容器临时 filesystem 的实现；文件主体已进入 MinIO。开发脚本中的绝对 Windows 路径数量较多，但未发现同类路径进入正常业务 Controller/Service。

## 10. Authentication / CORS / HTTPS Audit

- 前端 Axios `withCredentials: true`（`http.js:9`）。
- `AuthService.java:121-122,131-132` 创建 HttpOnly refresh cookie，`SameSite=Strict`，path 为 `/api/v1/auth`，未设置固定 cookie Domain（host-only）。这适合 Quick Tunnel 每次域名变化时的同源请求。
- `application.yml:76` 的 `APP_REFRESH_COOKIE_SECURE` 默认 `false`，仅适用于本地 HTTP 开发。Cloudflare 公网 HTTPS 部署必须显式设为 `true`，否则 cookie 安全属性不符合公网场景，可能影响 refresh 行为。
- 当前 CORS 默认仅 `http://localhost:5173`（`application.yml:77`）。若浏览器仅访问同源 Nginx `/api`，CORS 一般不参与；若保留任何跨源入口，必须仅允许明确的公共 HTTPS origin，不能与 credentials 组合使用泛 wildcard。
- 已有 forwarded headers 支持（`application.yml:4`），未来仍须用公网 HTTPS 登录/刷新进行实测，确认 `X-Forwarded-Proto` 不会被错误解读为 HTTP。

## 11. AMap / External Service Audit

- 后端高德 Web Service 使用 HTTPS `https://restapi.amap.com`（`application.yml:94`），前端 JS loader 亦为 HTTPS（`amapLoader.js:15`）；未发现当前 mixed-content 代码路径。
- 前端 Key/security code 位于被忽略的 `frontend/.env.local`，后端 Web Service key/private signing material 通过配置环境变量进入；本文不展示真实值。
- AMap Web JS Key 的域名白名单状态无法由项目源码验证。Quick Tunnel 的随机 `trycloudflare.com` 域名每次可能改变，若高德控制台不接受该 host，地图会失败。这是 **P1**；若课堂演示必须展示地图，则应按 P0 处理，优先使用可预测的 tunnel/public hostname 或确认白名单规则。
- 未发现其他必须配置固定 callback/redirect URL 的第三方服务。

## 12. Secret & Environment Configuration Audit

| Secret / Config | 当前来源 | 已被 Git ignore | Docker 后建议来源 |
|---|---|---|---|
| MySQL URL、用户名、密码 | `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`；本地环境文件 | 是 | PUBLIC_DEMO 专用 `.env`/Docker secrets，不入镜像 |
| JWT secret / issuer / audience | `APP_JWT_*` 环境配置 | 本地环境文件是 | Docker secrets/环境变量；稳定且不输出 |
| MinIO access / secret key | `MINIO_ACCESS_KEY`、`MINIO_SECRET_KEY` | 本地环境文件是 | Docker secrets/环境变量 |
| Redis password | `REDIS_PASSWORD` | 本地环境文件是 | Docker secret/环境变量；无密码则明确空值 |
| 高德 Web/JS 凭据 | `AMAP_*`、`VITE_AMAP_*` | `.env.local` 是 | 后端 runtime secret；前端 build-time secret/config |
| bootstrap account 密码 | `APP_BOOTSTRAP_*` | 本地环境文件是 | 仅演示环境受控变量；不写入代码 |

根 `.gitignore:1-11` 已覆盖 `deploy/local.env.ps1`、`deploy/local.env.properties`、`frontend/.env.local`、`deploy/runtime/`、backend target、frontend node_modules/dist。后续还应在 PUBLIC_DEMO 忽略 MySQL dump、MinIO export 与生产 `.env`；本轮不改 ignore。

## 13. Windows Docker Desktop Readiness

| 检查 | 结果 |
|---|---|
| `docker version` | 命令不可用，未检测到 Docker CLI |
| `docker compose version` | 命令不可用，Compose v2 不可用 |
| `wsl --status` | 当前系统提示需要安装 WSL；未达到可验证 WSL2 Docker 后端状态 |
| cloudflared | 未在 PATH 中发现 |

结论：当前 Windows Docker Desktop / Docker Engine / Compose / WSL2 **未就绪**。后续必须先由用户完成 Docker Desktop（WSL2 backend）安装与启动，再进行真实 compose 预检；本轮未安装、未启动、未修改系统环境。

## 14. Target Container Boundary Assessment

| Container | Internal Port | Host Port 是否需要 | 依赖 | 是否需要 Volume |
|---|---:|---|---|---|
| web（Vue build + Nginx） | 80 | 默认不需；cloudflared 走 Docker 网络 | backend | 否 |
| backend（Spring Boot） | 8080 | 否 | mysql、redis、minio | 否（业务文件已入 MinIO） |
| mysql（MySQL 8） | 3306 | 否 | 数据恢复完成后 backend | **是**，数据库 named volume |
| minio | 9000 | 否 | 数据恢复完成后 backend | **是**，对象 named volume |
| redis | 6379 | 否 | backend | 通常否；当前无业务 Redis 状态 |
| cloudflared | 无入站端口 | 否 | web | 否 |

公网入口应只有 cloudflared → `web:80`。MySQL、Redis、MinIO API、MinIO Console 和 backend 均不应直接发布宿主公网端口。若需本机排障，可仅临时绑定 `127.0.0.1`，不要绑定全网卡。

## 15. P0 / P1 / P2 Risk Register

### P0 — Docker/公网前必须解决

| 问题 | 定位 | 为什么当前可用 | Docker 后风险 | 后续处理类型 |
|---|---|---|---|---|
| backend 回环绑定 | `application.yml:3` | 浏览器、Vite 和 backend 同机 | Nginx 容器连接不到 backend | 生产 env 设置 `SERVER_ADDRESS=0.0.0.0` |
| 本地开发引导自动介入 | `SmartPropertyApplication.java:14`；`LocalDevelopmentServices.java:20`；`LocalDevelopmentConfiguration.java:23,38` | 本地配置/PowerShell 存在 | 容器误运行 Windows 本机启动/路径逻辑 | PUBLIC config 禁用 `APP_DEV_BOOTSTRAP`；后续把开发引导限制到 local profile |
| SPA history 深链接 | `frontend/src/router/index.js:52` | Vite 自动处理 | Nginx 刷新子路由 404 | Nginx `try_files` 回退 |
| 公网 refresh cookie | `application.yml:76`；`AuthService.java:121-132` | 本地 HTTP 需要 Secure=false | HTTPS 演示安全性与 refresh 流程异常 | production env `APP_REFRESH_COOKIE_SECURE=true` + HTTPS smoke |

### P1 — 不解决可能影响功能、演示或数据完整性

| 问题 | 定位 | 后续处理类型 |
|---|---|---|
| MySQL 快照漏 Flyway history / views / definer | DB `flyway_schema_history`、两个 view | 使用完整逻辑 dump；恢复后校验 history、view、definer 与权限 |
| 当前数据不能由空库重建 | migration、sample-data 与累计 demo data | 先全量 DB snapshot，绝不空库初始化 |
| MinIO objects/bucket policy 未复制 | `StorageAvailabilityService.java:47-50` | 先 restore bucket/object，再启动 backend；复制 policy/CORS（如当前存在） |
| AMap 随机 Quick Tunnel 域名白名单 | `frontend/src/map/amapLoader.js:4-21` | 使用固定可白名单 hostname 或先验证控制台允许规则 |
| CORS 仍只有 localhost | `application.yml:77` | 以同源 `/api` 为主；任何跨源场景需受限 origin |
| Nginx 上传限制 | `application.yml:40-41` | 设置 `client_max_body_size` ≥ 12MB |
| 容器时区/字符集/SQL mode 差异 | JDBC `Asia/Shanghai` 与 MySQL 当前事实 | MySQL 8、utf8mb4、collation/SQL mode/TZ 对齐 |

### P2 — 不阻断首轮课堂演示

| 问题 | 定位 | 建议 |
|---|---|---|
| PowerShell 与本机 executable 绝对路径 | `deploy/*.ps1`、本地环境文件 | 保留给原生开发；PUBLIC_DEMO 不使用 |
| runtime PID/logs 目录 | `deploy/runtime`、localdev 类 | 不复制 runtime 状态；容器日志走 stdout |
| 当前无正式 production profile | `application.yml` 与本地环境文件 | 后续增加独立 public/docker profile，而非污染本地环境 |

## 16. Exact Files That Will Need Modification

> 本节是下一轮 PUBLIC_DEMO 副本的修改清单，不在当前稳定工程执行。

| 文件 | 当前问题 | 后续修改目的 | 风险 |
|---|---|---|---|
| `backend/src/main/resources/application.yml` 或新增 public profile | 默认 localhost、cookie/CORS/Flyway 值为本地目标 | 生产 env 覆盖 server bind、DB/Redis/MinIO host、cookie secure、origin 与 Flyway 策略 | P0/P1 |
| `backend/src/main/java/.../SmartPropertyApplication.java` | 无条件 local dev bootstrap | 使 local 服务准备逻辑只在明确开发模式启动 | P0 |
| `backend/src/main/java/.../infrastructure/localdev/LocalDevelopmentServices.java`（可能） | 自动发现并运行本机开发链 | 增强 production guard，避免容器误触发 | P0 |
| `frontend` 生产 build 环境文件（新 PUBLIC_DEMO secret config） | AMap VITE 值为 build-time | 受控注入 JS key/security code，不进入仓库 | P1 |
| future Nginx config（新文件） | 当前无 Nginx | SPA fallback、`/api` 反代、body size、forward headers | P0/P1 |
| future Dockerfile/compose（新文件） | 当前不存在容器定义 | 定义独立服务网络、healthchecks、named volumes、无公网 DB/MinIO ports | P0 |
| PUBLIC_DEMO `.gitignore`（如缺少） | 未明确覆盖 dump/export | 忽略 public secret、DB dump、MinIO export | P1 |

## 17. MySQL Data Migration Requirements

后续应从当前 Windows `community_db` 取得**一致性逻辑快照**，恢复到独立 Docker MySQL 8。迁移前/后都应只在复制环境验证，不影响当前本机库。

必须保留：

1. 全部 47 张 base table 与全部行，包括 528 套房、人员、账单、表计、角色/权限、通知、户型图关联等演示状态。
2. 全部索引、外键、CHECK/约束、AUTO_INCREMENT 当前位置。
3. `v_bill_financial_summary` 与 `v_monthly_collection_summary`，并验证 DEFINER/权限。
4. `flyway_schema_history` 全部 V1–V10 成功记录。
5. `utf8mb4`、`utf8mb4_0900_ai_ci`、MySQL 8.0 兼容版本、SQL mode 与 Asia/Shanghai 业务时区。

恢复顺序建议：停止于“只读预检”后，先创建 PUBLIC_DEMO Docker MySQL volume，恢复完整 dump，运行只读 count/view/Flyway 校验，确认无误后才启动 backend；首启不要把 Flyway 当作重新建库工具。完成前不得重置、清空、覆盖当前 Windows `community_db`。

## 18. MinIO Data Migration Requirements

后续迁移需要完整复制 `property-assets` bucket 及其 object key，保持 `file_object.bucket_name`/`object_key` 与对象存储中的对象逐一对应。DB 无 endpoint URL 的设计意味着 Docker endpoint 改为 `http://minio:9000` 不会破坏关联。

必须满足：

1. 从当前 MinIO 进行只读 inventory：bucket 名、对象数量、key、大小/etag（后续执行，不是本轮）。
2. 在 Docker MinIO 创建/恢复同名 bucket 与完全相同 key；不要改变 `property-units/...` 等 key 前缀。
3. 复制 bucket policy 与 CORS（如当前存在）；`StorageAvailabilityService` 的自动建 bucket不能替代历史对象/策略迁移。
4. 数据库 snapshot 与对象 snapshot 应在同一维护时间点取得，防止 DB 已写 file_object 但对象未拷贝，或反向出现孤儿对象。
5. 恢复完成后用已知户型图 API 做只读下载验证；浏览器仍应只访问 `/api/.../floor-plan`，不直连 `minio:9000`。

## 19. Minimal Public Deployment Roadmap

1. **冻结当前本地稳定版**：仅复制目录，保留 Windows MySQL/MinIO/Redis 与源码不动。
2. **准备 Docker Desktop/WSL2**：安装并启动由用户控制；确认 Docker Engine、Compose v2 可用。
3. **制作一致性备份计划**：先 inventory MySQL 与 MinIO，再取得完整 DB dump + MinIO object/policy snapshot。
4. **创建独立 PUBLIC_DEMO 副本**：不带 `node_modules`、target、dist、runtime、local secrets、MinIO data。
5. **在 PUBLIC_DEMO 内创建 production config**：禁用 local bootstrap；backend 改 internal service names；启用 HTTPS refresh cookie；配置 AMap build/runtime credentials。
6. **创建 web/backend/mysql/minio/redis/cloudflared 服务定义**：无公网发布 DB/Redis/MinIO/backend，增加 healthchecks 和 MySQL/MinIO named volumes。
7. **恢复 MySQL，再恢复 MinIO**：验证 count、Flyway history、views、file object keys/下载。
8. **Nginx 本机同源验收**：登录、刷新 `/map`、上传/下载户型图、统计、居民/物业角色与 AMap。
9. **Quick Tunnel 验收**：仅指向 web；用公网 HTTPS 复测 refresh cookie、forwarded headers、AMap 域名允许情况。

## 20. No-Change Verification

执行前 `git status --short` 与执行后 `git status --short` 均无法在目标目录完成：该目录及其父目录未包含 `.git`，Git 返回“not a git repository”。因此不能伪造 Git clean 结论。

本轮实际操作仅为只读检查与新增本报告。除 `docs/PUBLIC_DEPLOYMENT_PRECHECK.md` 外，未修改任何现有 Java、Vue、SQL、XML、YAML、环境配置、数据库、MinIO、Redis、PowerPoint、启动脚本或 Docker 文件；未创建 Dockerfile、Compose 或 Nginx 配置，未启动/停止任何服务。
