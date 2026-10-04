# Engineering Stabilization V2 交付报告

完成时间：2026-08-22  
范围：只稳定现有 Phase 2A；未进入 Phase 2B / Phase 2C；未修改数据库 Schema。

## 1. 最终结论

本轮结果：**PASS**。

- 日常开发明确拆分为 IDEA 后端与 WebStorm 前端。
- 8080 只提供 Spring Boot API，根路径返回 404；唯一开发页面是 5173。
- 默认 Maven 构建不再复制 `frontend/dist`，后端不会伪装成第二套前端。
- 所有业务 JSON 统一为 `ApiResponse<T>`，普通对象、集合和分页对象都位于 `data`。
- 全局异常、认证失败、权限拒绝均返回相同 JSON 结构并包含 `traceId`。
- 后端日志、访问日志、前端行为日志、启动日志按每次运行独立保存。
- IDEA 停止、脚本 Ctrl+C 和联合启动器均按 PID + 可执行文件 +启动时间精确回收。
- Redis 是可降级性能层；停止 Redis 后业务 API 仍直接读取 MySQL。
- MinIO 上传、下载、权限和清理完成真实回归。
- Microsoft Edge 页面链路与后端停机自动登出完成真实回归。

## 2. 两个“前端”问题的根因

此前后端 Maven 资源配置把 `../frontend/dist` 复制到 `backend/target/classes/static`，Spring Boot 又把 `static/index.html` 当作欢迎页。因此：

1. Vite 5173 是源码开发页面；
2. Spring Boot 8080 又暴露了一份旧的打包页面；
3. 即使 Vite 未启动，浏览器仍可能看到 8080 中残留的旧页面；
4. `target/classes/static` 的过期文件还会让修改后的源码与浏览器显示不一致。

修复：

- 从 `backend/pom.xml` 删除开发期前端资源复制。
- 删除后端 SPA 转发控制器。
- 清理旧的 `backend/target/classes/static` 生成物。
- 将 `frontend/dist` 加入忽略范围。
- 实测 `http://127.0.0.1:8080/` 返回 404，`http://127.0.0.1:5173/` 才是开发页面。

## 3. 端口占用与残留进程根因

### 3.1 8080 已占用

原始问题不是 Spring Boot 随机失效，而是上一轮 IDEA Java Run 的 `SmartPropertyApplication` 子 JVM 仍在监听 8080。旧状态文件无法可靠识别该进程，新的后端因此启动失败，但浏览器仍能调用旧 JVM。

现在：

- 启动前只读检查 8080；占用时报告 PID 与命令，不自动杀进程。
- 项目自启进程记录 PID、可执行文件、启动时间、运行 ID 和所有者令牌。
- 停止时只处理完全匹配的项目进程。
- 联合启动器另外在内存中保留已核验的 Java/Node 身份；即使 JVM shutdown hook 提前删除记录，仍能回收 Maven 子 JVM。
- 最终复验 Ctrl+C 输出：后端 PID 7968、前端 PID 10636、MinIO PID 12228、Redis PID 23668 均被精确停止。

### 3.2 前端 `-1073740791`

直接 `npm run dev` 和修复后的前端脚本均可稳定运行，Windows 事件日志没有 Node/Vite 自身崩溃记录。原问题来自旧的清理/包装流程：Node 被外部终止后，包装脚本把原生命令的强制退出码统一解释成“Vite 启动失败”。

现在前端脚本只负责 Vite；后端脚本只负责 Java；联合脚本才负责集中编排。端口占用直接报告，不会按名称批量结束 Node/Java。

## 4. 最终启动架构

### 默认日常开发

1. IDEA 打开后端 Maven 模块，运行 `SmartPropertyApplication.main()`。
2. WebStorm 打开 `frontend`，运行 npm 脚本 `dev`。
3. 浏览器只打开 `http://127.0.0.1:5173`。

后端 Java Main 会：

- 读取 `deploy/local.env.properties`；
- 检查 8080 与唯一的 `community_db`；
- 复用或启动 Redis/MinIO；
- 启动 Spring Boot；
- 停止时只回收本次拥有的依赖 PID。

前端 Vite 会：

- 监听 127.0.0.1:5173；
- 热更新 Vue 源码；
- 把 `/api` 代理到 127.0.0.1:8080；
- 不启动、不停止后端。

### 可选联合模式

```powershell
.\deploy\start-dev.ps1 -SkipInstall
```

该模式用于集中观察日志，不是默认 IDE 工作流。Ctrl+C 后必须看到每个本次拥有的 PID 被停止。

## 5. 后端目录结构

```text
com.smartproperty.platform
├─ asset
│  ├─ controller
│  ├─ service
│  ├─ mapper
│  ├─ model
│  └─ dto
│     ├─ request
│     └─ response
├─ auth
│  ├─ controller
│  ├─ service
│  ├─ mapper
│  ├─ model
│  └─ dto
├─ file
│  ├─ controller
│  ├─ service
│  ├─ mapper
│  └─ model
├─ common
│  ├─ api
│  ├─ exception
│  └─ web
└─ infrastructure
   ├─ config
   ├─ diagnostics
   ├─ localdev
   ├─ observability
   ├─ security
   └─ storage
```

资产域不再把 Controller、Service、Mapper、请求 DTO、响应 DTO 和数据库实体扔在同一目录。Mapper 依赖 model/entity，不依赖 Controller DTO。没有为形式而增加空的 `ServiceImpl` 接口层。

## 6. 统一 HTTP 返回

所有业务 JSON 使用：

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "请求成功",
  "data": {},
  "fieldErrors": null,
  "status": 200,
  "traceId": "12位追踪号",
  "timestamp": "带时区时间"
}
```

规则：

- 普通对象放在 `data`。
- 集合放在 `data`。
- 分页对象整体放在 `data`，其中再包含 `items/total/page/pageSize`。
- 失败时 `success=false`，使用明确的业务 `code/message/status/traceId`。
- 二进制文件下载保持真实 `image/png` 等内容，不套 JSON。
- 前端 Axios 拦截器读取 envelope 后，把 `response.data` 指向 envelope 的 `data`，现有页面不必重复写 `.data.data`。

已实际覆盖：200、201、400、401、403、404、405、409、413、500。

## 7. 全局异常与日志

### 后端处理

- 参数校验/错误 JSON：400。
- 未认证：401。
- 无权限：403。
- 资源不存在：404。
- 方法不允许：405。
- 数据冲突/乐观锁冲突：409。
- 文件过大：413。
- 未知异常：500，响应不泄露堆栈，后端日志保存完整堆栈。

受控 500 探针实测：

```text
HTTP 500
success=false
code=INTERNAL_ERROR
traceId=F90194264BB5
```

访问日志：

```text
user=admin POST /api/v1/dev/exception-probe status=500 duration=3ms trace=F90194264BB5
```

后端日志保存了 `IllegalStateException` 及 `ExceptionProbeController.java:18` 开始的完整调用栈。探针默认关闭，仅本地显式启用。

### 日志文件

每次运行保存到：

```text
logs/run-<RUN_ID>/
├─ backend.log
├─ access.log
├─ frontend-client.log
└─ startup.log
```

- `access.log` 记录用户、方法、路径、状态、耗时、traceId；查询值被过滤。
- `frontend-client.log` 记录点击、提交、路由、前端异常和 HTTP 失败，不记录密码、JWT、Cookie 或请求正文。
- 控制台 2xx 绿色、4xx 黄色、5xx 红色。
- 客户端事件接口只在本地启用，只接受 loopback，并进行限流。

Microsoft Edge 回归共观察到 34 条语义行为日志，包括从资产工作台进入人员、租赁、收费、地图、设置、消息中心再返回资产页。

## 8. Redis 降级验证

本轮精确停止项目自启 Redis PID 37120，确认 6379 不可连接后再次请求资产概览：

```text
RedisPortReachable=false
ApiStillHttpSuccess=true
ApiCode=SUCCESS
DataPresent=true
```

结论：Redis 不影响数据正确性；不可用时业务直接访问 MySQL，只损失缓存性能。

## 9. MinIO 验证

通过 5173 → 8080 → MySQL/MinIO 的真实链路完成：

- 管理员创建 ESV2 临时小区、楼栋、单元、房产。
- 上传 68 字节 PNG，返回统一 envelope，`data.contentType=image/png`。
- 只读游客下载：HTTP 200、`image/png`、68 字节一致。
- 只读游客上传：`403/ACCESS_DENIED`。
- 对象存储使用私有桶 `property-assets`。
- 测试后按精确 bucket/object key 删除 MinIO 对象。
- `file_object` 测试记录为 0，ESV2 临时业务行为 0，12 条变更审计保留。

## 10. Edge 与心跳验证

Edge 端到端回归：

```text
EDGE_E2E_PASS
/assets /people /rentals /fees /map /settings /notifications
```

后端停机测试：

1. Edge 登录并进入 `/assets`。
2. 单独停止 IDEA 等价后端，保持 Vite 运行。
3. 连续三次心跳失败后跳转到 `/login?offline=1&redirect=/assets`。
4. 自动化输出 `BACKEND_STOP_AUTO_LOGOUT_PASS`。

## 11. 完整回归结果

| 检查项 | 结果 |
|---|---|
| Maven 主代码与测试代码编译 | PASS |
| IDEA 等价 Java Main 启动 | PASS |
| Vue/Vite 单独启动 | PASS |
| 联合启动与 Ctrl+C 精确回收 | PASS |
| 8080 根路径无前端 | PASS（404） |
| 5173 页面与 API 代理 | PASS |
| 统一成功/分页/错误 JSON | PASS |
| JWT 登录、刷新、退出、退出后刷新失败 | PASS |
| 游客只读与管理员写权限 | PASS |
| 资产四级 CRUD | PASS |
| 唯一约束与乐观锁冲突 409 | PASS |
| 400/401/403/404/405/409/413/500 | PASS |
| 完整 500 堆栈与 traceId | PASS |
| Redis 不可用回退 MySQL | PASS |
| MinIO 上传、下载、权限、清理 | PASS |
| Edge 7 路由 | PASS |
| 前端行为日志 | PASS |
| 后端停机自动登出 | PASS |
| ESV2 业务测试数据清理 | PASS（0 行） |
| 最终端口状态 | PASS（8080/5173/6379/9000/9001 全部 FREE） |

## 12. 现在如何使用

### 后端

在 IDEA 中运行：

```text
backend/src/main/java/com/smartproperty/platform/SmartPropertyApplication.java
```

看到 `Web UI http://127.0.0.1:5173 (start Vue separately)` 表示后端准备完毕；这不是让后端启动 Vue，而是告诉开发者应在 5173 打开页面。

### 前端

推荐安装并使用 JetBrains WebStorm，打开 `frontend`，运行 npm 的 `dev` 脚本；或在 WebStorm 终端执行：

```powershell
npm run dev
```

浏览器打开：

```text
http://127.0.0.1:5173
```

### 状态与停止

```powershell
.\deploy\status-dev.ps1
.\deploy\stop-development.ps1
```

已知归属的项目进程会精确停止；未知端口监听者只报告，不会自动处理。

## 13. 本轮未做事项

- 未进入 Phase 2B / Phase 2C。
- 未修改业务 Schema。
- 未创建临时数据库。
- 未执行最终前后端合包。
- 未部署公网。
- 未记录或输出任何密码、JWT、Cookie。

