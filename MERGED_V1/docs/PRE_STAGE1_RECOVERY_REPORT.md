# Pre-Stage1 独立恢复报告

> 恢复日期：2026-08-24  
> 源目录（只读保护）：`C:\Projects\数据库课程设计\多小区智慧物业管理系统`（`CURRENT_STAGE1`）  
> 恢复目录（本轮唯一写入目录）：`C:\Projects\数据库课程设计\多小区智慧物业管理系统_PRE_STAGE1_RECOVERED`（`PRE_STAGE1_RECOVERED`）

## 1. 恢复边界与结果

本轮没有修改 `CURRENT_STAGE1`，没有创建融合版，也没有向 `community_db` 写入任何内容。

恢复目录由当前工程主体复制建立，再仅在副本内按模块恢复 Pre-Stage1 连接关系。复制时排除了 IDEA/日志/运行时数据；复制工具意外带入的 `node_modules`、`dist`、`backend/target` 等本地生成物已只在恢复副本内清理，随后在恢复副本内为前端 build 重新生成。

本恢复目录不保留 Stage 1 Admin MVP 的 `/api/admin/**`、Dashboard、账单页面、统计页面、ECharts、Mock `SUPER_ADMIN` 和 Stage 1 Mapper。

## 2. 恢复来源与优先级执行结果

| 来源 | 结果 | 说明 |
|---|---|---|
| IDE Local History | 已检查，未直接恢复原文 | 找到 IntelliJ / WebStorm Local History 存储和项目路径索引，但命令行无法可靠导出文件正文；没有把二进制索引误当源码。 |
| Codex Stage 1 修改前上下文与修改记录 | 15 个文件按已知 Stage 1 精确改动反向恢复 | 这些文件的 Stage 1 改动内容、位置和原语义在实施记录中可确认。 |
| 当前残留旧源码和调用关系 | 4 个前端连接文件恢复 | `stores/auth.js`、`clientMonitor.js`、`LoginView.vue`、`AssetsView.vue`、`api/assets.js` 明确约束了 HTTP、认证、路由和布局需要恢复的契约。 |
| 等价恢复 | 4 个文件/行为 | 旧原文无法安全导出时，以已知行为恢复，不冒充为逐字旧文本。 |

## 3. 从 Codex 旧上下文/修改记录精确反向恢复的文件（15）

1. `backend/src/main/java/com/smartproperty/platform/SmartPropertyApplication.java`：恢复普通 `@SpringBootApplication`，去除全局 Security 自动配置排除。
2. `backend/src/main/java/com/smartproperty/platform/infrastructure/security/SecurityConfig.java`：恢复默认装配。
3. `backend/src/main/java/com/smartproperty/platform/auth/controller/AuthController.java`：恢复默认注册 `/api/v1/auth/**`。
4. `backend/src/main/java/com/smartproperty/platform/auth/service/AuthService.java`：恢复默认装配。
5. `backend/src/main/java/com/smartproperty/platform/auth/service/JwtService.java`：恢复默认装配。
6. `backend/src/main/java/com/smartproperty/platform/auth/service/AccountBootstrapRunner.java`：恢复默认装配。
7. `backend/src/main/java/com/smartproperty/platform/infrastructure/localdev/LocalDevelopmentServices.java`：恢复 MySQL → Redis → MinIO 的默认准备顺序。
8. `backend/src/main/java/com/smartproperty/platform/infrastructure/storage/MinioConfig.java`：恢复默认 MinIO Client 装配。
9. `backend/src/main/java/com/smartproperty/platform/infrastructure/storage/StorageAvailabilityService.java`：恢复默认存储状态检查。
10. `backend/src/main/java/com/smartproperty/platform/file/controller/FloorPlanController.java`：恢复默认户型图接口注册。
11. `backend/src/main/java/com/smartproperty/platform/file/service/FloorPlanService.java`：恢复默认户型图服务装配。
12. `backend/src/main/java/com/smartproperty/platform/infrastructure/diagnostics/RedisAvailabilityService.java`：恢复默认 Redis 状态检查。
13. `backend/src/main/java/com/smartproperty/platform/infrastructure/diagnostics/SystemStatusController.java`：恢复 `/api/v1/system/heartbeat`。
14. `backend/src/main/java/com/smartproperty/platform/infrastructure/diagnostics/StartupSummaryReporter.java`：恢复启动摘要、服务状态输出和 PID 记录入口。
15. `backend/src/main/resources/application.yml`：移除 Stage 1 的 `legacy-*` 默认关闭开关及 Mock Admin 配置。

## 4. 从当前残留旧调用关系恢复的文件（4）

| 文件 | 恢复内容 | 调用关系依据 |
|---|---|---|
| `frontend/src/api/http.js` | `/api/v1`、Bearer Token、Refresh Token、401 重试、认证更新通知、HTTP 失败监听、响应 envelope 解包 | `stores/auth.js` 需要 `setAccessToken`/`onAuthUpdate`；`clientMonitor.js` 需要 `onHttpFailure`；`api/assets.js` 需要 `/api/v1` 共享客户端。 |
| `frontend/src/router/index.js` | `/login`、`/assets`、人员/居住、租赁/合同、收费、地图、消息、系统设置以及认证守卫 | `LoginView.vue`、`AssetsView.vue`、`PhasePlaceholderView.vue` 与监控离线跳转均使用这些路径。 |
| `frontend/src/main.js` | 恢复 `useAuth()` 与 `initializeClientMonitoring(...)` 初始化 | `clientMonitor.js` 的行为依赖 app、router、auth 三者。 |
| `frontend/src/layouts/AppLayout.vue` | 恢复旧应用壳：左侧菜单、登录账号、退出入口、消息入口、服务状态与 Phase 2A 信息 | Pre-Stage1 页面、认证状态和此前人工验收截图所反映的原交互结构。 |

## 5. EQUIVALENT_RECONSTRUCTION（4）

以下内容没有取得可验证的逐字 Local History 原文，因此只恢复已知行为，并保留原有技术路线，没有进行架构升级：

1. `LocalDevelopmentConfiguration.java`：恢复“本地未设置 JWT 密钥时生成仅进程内开发密钥”的行为；
2. `frontend/src/api/http.js`：依据旧调用方恢复 Axios/JWT/Refresh 运行契约；
3. `frontend/src/router/index.js`：依据旧页面、路径和认证流程恢复路由守卫与占位入口；
4. `frontend/src/layouts/AppLayout.vue`：依据已存在旧页面、旧功能菜单和此前视觉/交互信息恢复应用壳。

这些文件不应被描述为“已从 IDE Local History 原文还原”。

## 6. Stage 1 专属内容在恢复副本中的处理

以下内容仅从 `PRE_STAGE1_RECOVERED` 副本移除；`CURRENT_STAGE1` 完全保留：

- 后端：`auth/context/**`（Mock Actor）、`community/**`、`property/**`、`billing/**`、`dashboard/**`、`statistics/**`、`common/api/PageResult.java`、`resources/mapper/stage1/**`；
- 前端：Stage 1 的五个 API 文件、`EChart.vue`、Dashboard/Property/Billing/Statistics 页面；
- 构建依赖：`echarts` 与对应 `package-lock.json` 项；
- 脚本：`scripts/smoke-test.ps1`。

恢复副本静态检查结果：`STAGE1_SOURCE_RESIDUE=NONE`。

## 7. 已恢复能力

### Authentication / JWT / Security

- Spring Boot 不再全局排除 `SecurityAutoConfiguration`；
- 原 `SecurityConfig`、`AuthController`、`AuthService`、`JwtService`、`AccountBootstrapRunner` 默认参与 Bean 装配；
- `/api/v1/auth/login`、refresh、logout、me 的后端链路恢复；
- 前端 HTTP 客户端恢复 Bearer Token、Refresh Cookie、401 refresh 重试与认证状态通知；
- 前端 `/login`、认证守卫、登录后跳转 `/assets` 恢复。

### Router / Login / Layout

- 恢复 `/login`、`/assets`、`/residents`、`/rentals`、`/fees`、`/map`、`/notifications`、`/settings`；
- 恢复 `AssetsView` 为默认登录后首页；
- 恢复旧布局中的导航、账号显示、退出登录、消息入口和服务状态；
- 旧的人员、租赁、收费、地图、消息、设置占位入口被保留，不伪造新业务数据。

### Redis / MinIO / 户型图

- 恢复本地开发启动时依次准备 MySQL、Redis、MinIO；
- 恢复 `RedisAvailabilityService`、`SystemStatusController` 与 `StartupSummaryReporter`；
- 恢复 `MinioConfig`、`StorageAvailabilityService`、`FloorPlanController`、`FloorPlanService`；
- 恢复 `FileObjectMapper` 现有连接链使用的默认装配。

### 启动控制台、PID 与 System Status

- 恢复 `StartupSummaryReporter` 运行，输出 MySQL、Redis、MinIO、JWT、前端地址状态；
- 恢复其对 `LocalDevelopmentServices.recordBackendProcess(...)` 的 PID 记录入口；
- 恢复 `/api/v1/system/heartbeat` 与前端离线检测所使用的旧协议；
- 恢复 `main.js` 对客户端点击、HTTP 失败、前端异常和心跳的初始化。

### 资产页面/API

- 保留原 `asset/**` Controller / Service / Mapper / XML；
- 恢复 `AssetsView.vue`、`AssetNodeDialog.vue`、`PropertyDrawer.vue` 与 `api/assets.js` 所需的 `/api/v1` HTTP 调用；
- 恢复户型图上传、下载/签名访问链的 Bean 注册条件。

## 8. 数据库、迁移与安全边界

- 本轮没有启动 Spring Boot，因此没有触发账号初始化、Refresh Session 写入或运行时数据库行为；
- 没有执行 Flyway migrate / clean / seed；
- 没有执行 MySQL 的 INSERT、UPDATE、DELETE、ALTER、DROP、TRUNCATE；
- `database/migrations/V1__init_schema.sql`、`V2__seed_reference_data.sql`、`V3__phase2a_security_assets_and_files.sql` 原样保留，未修改；
- Redis、MinIO 本地服务恢复了代码和启动链，但本轮没有为了证明恢复而启动或停止任何服务。

## 9. 验证结果

| 项目 | 结果 | 备注 |
|---|---|---|
| 后端 `mvn compile` | PASS | 仅编译恢复副本，未启动应用。 |
| 前端 `npm run build` | PASS | 仅恢复副本；存在既有 Rollup `#__PURE__` 注释与 bundle size 警告，不是失败。 |
| Stage 1 专属源码残留 | NONE | 静态检索无 `/api/admin`、`legacy-*`、Mock Actor、ECharts 等残留。 |
| 数据库写入 | NO | 未执行数据库写操作。 |
| CURRENT_STAGE1 关键文件校验 | MATCH | 11 个关键源码/配置/迁移文件 SHA-256 与建立恢复目录前完全一致。 |

## 10. 未能精确恢复或待人工验证的内容

1. IDE Local History 找到存储但无法安全抽取旧文本，因此 HTTP、Router、Layout 与 JWT 本地兜底被明确标记为等价恢复；
2. 本轮按纪律没有运行 Spring Boot，故真实登录、Refresh Cookie、Redis/MinIO 连接、户型图上传下载与 PID 回收待人工启动验证；
3. 没有 Git 基线，无法提供提交级回滚证明；后续在两个独立版本确认后应建立 Git 基线；
4. 恢复版的 `frontend/dist` 与 `frontend/node_modules` 是本副本的本地验证产物，不是来源代码历史；
5. 当前不做 Stage 1 + Pre-Stage1 合并，任何 `/api/admin/**` 或 Dashboard 功能均不应被带入本目录。

## 11. 最终边界声明

```text
CURRENT_STAGE1_MODIFIED = NO
PRE_STAGE1_RECOVERED = ESTABLISHED
DATABASE_MODIFIED = NO
OTHER_PARENT_FILES_MODIFIED = NO
```

本报告是 `PRE_STAGE1_RECOVERED` 内的恢复记录。下一轮如需合并，必须以 `CURRENT_STAGE1` 和本恢复目录作为两个独立事实源重新讨论，不得将本轮等价恢复误认为 Stage 1 原版。
