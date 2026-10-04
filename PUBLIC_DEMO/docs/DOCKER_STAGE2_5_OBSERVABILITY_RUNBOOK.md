# Stage 2.5：本机日志、审计与可观测性运行手册

## 边界

- 本阶段仍仅供本机验收；Web 保持 `127.0.0.1:18080`，未开启 LAN 或公网访问。
- 数据库、Redis、MinIO 继续使用既有 Docker named volumes；本手册中的操作不使用 `down -v`。
- 日志根目录：`C:\Projects\数据库课程设计\deployment_logs\smart_property_public_demo`。
- 账号密码、JWT、Cookie、数据库/MinIO 凭据不会写入日志、部署流水或本手册。

## 日志目录

```text
deployment_logs/smart_property_public_demo/
  nginx/access/access.jsonl        # 访问 JSONL，含 Nginx request_id
  nginx/error/error.log             # Nginx 错误
  backend/application/              # 应用运行日志，按天保留
  backend/http/http.jsonl           # 安全 HTTP 请求/响应事件
  backend/trace/trace.jsonl         # Controller/Service/Mapper 调用链
  audit/audit.jsonl                 # 成功写操作审计
  security/security.jsonl           # 登录、拒绝访问与认证事件
  mysql/error/mysql-error.log       # MySQL 错误
  mysql/slow/mysql-slow.log         # MySQL 慢查询（阈值 0.5 秒）
  ops/docker-events/                # Docker 生命周期事件
  ops/metrics/                      # 每分钟 docker stats / system df
  ops/agent/security-alerts.jsonl   # 轻量安全汇总
  deployments/deployments.jsonl     # 手工后端/前端部署记录
```

后端处理 API 请求时会将 Nginx 生成的 `X-Request-ID` 放入 MDC，并回传该响应头。日志中同时记录 deployment ID 和 Docker `HOSTNAME` 作为 instance ID。转发头只保存原始值，并且一律标记为未受信任；它们不会被用来覆盖客户端 IP。

## 日常操作

在项目根目录执行：

```powershell
.\scripts\observability\start-observability.ps1
.\scripts\observability\status-observability.ps1
.\scripts\observability\stop-observability.ps1
```

采集器默认在后台运行，记录 Docker 事件、每分钟资源样本，并在五分钟窗口中把达到阈值的 4xx/5xx 或可疑路径写入安全汇总。它不执行封禁、不自动杀进程、不改变容器。

可按需查询：

```powershell
.\scripts\observability\find-request.ps1 -RequestId <X-Request-ID>
.\scripts\observability\find-user.ps1 -Username <username>
.\scripts\observability\find-ip.ps1 -Ip <ip>
.\scripts\observability\recent-errors.ps1
.\scripts\observability\log-status.ps1
```

## 手工服务替换

仅替换 backend 或 web 时使用：

```powershell
.\scripts\docker\deploy-service.ps1 -Service backend
.\scripts\docker\deploy-service.ps1 -Service web
```

该工具会生成 deployment ID、记录旧/新容器与镜像 ID、构建目标镜像、仅重建目标服务并等待健康检查；不会执行 `down`、不会影响数据库卷。后端替换后，新的 deployment ID 与新的 Docker instance ID 会出现在后续请求日志中。

## 保留与排障

- 采集器每日清理超过 90 天的日志文件；容器数据卷不属于清理目标。
- 若 Nginx 或后端没有写日志，先执行 `docker compose --env-file .env.docker ps`，然后检查 `log-status.ps1` 输出和相应容器健康状态。
- 若 MySQL 不能启动，先查看 `mysql/error/mysql-error.log`；不要删除 named volume 作为排障动作。
- 若需要完整重启应用服务，可以使用 `docker compose --env-file .env.docker down` 后再 `up -d`，但严禁附加 `-v`。

## Windows 登录后自动启动

已注册的当前用户计划任务名称为 `SmartPropertyPublicDemoObservability`。它在当前用户 **AtLogOn** 时以交互式、非管理员权限运行：

```text
powershell.exe -NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass
  -File scripts\observability\observability-bootstrap.ps1
```

任务不会启动 Docker、构建镜像、拉取镜像、执行 `compose up/down` 或修改业务容器。五项服务仍由既有 `restart: unless-stopped` 机制恢复；该任务只观察和采集。

Bootstrap 以 10 秒间隔用 `docker version` 检查 Docker Engine 是否可通信；连续等待超过 15 分钟后降为每 60 秒一次。Docker 未就绪时会写 `WAITING_FOR_DOCKER`；就绪后写 `DOCKER_READY` 并用只读 `docker compose ps` 确认项目可识别，再启动已有 events/metrics 采集器。Docker 中途重启时会记录 `DOCKER_CONNECTION_LOST`，恢复后记录 `DOCKER_CONNECTION_RESTORED`。同一用户使用 Windows named mutex、计划任务 `IgnoreNew` 设置和既有 worker PID 文件三层防止重复采集。

### Run-session classification (Stage 2.6 correction)

`Event != Run Session`. `HOST_BOOT` is now tied to the one `logs/_runtime/host-session.json` marker for the active Windows login/host session. Bootstrap, scheduled-task reattachment, Docker readiness, collector restart/reconnect and container health transitions only append lifecycle records to the active run's `ops/lifecycle`; they never create a host-boot directory. Only a real manual deployment or verified unexpected recovery creates a normal runtime run. Load tests are evidence beneath the existing runtime (`tests/system-load-*` or `tests/database-load-*`), never normal run creators. Runtime pointers and agent control files are under `logs/_runtime`; old historical directories are preserved and not cleaned by collectors.

Lifecycle 日志位于：

```text
C:\Projects\数据库课程设计\deployment_logs\smart_property_public_demo\ops\agent\agent-lifecycle-YYYYMMDD.log
```

手工管理与任务管理：

```powershell
# 手工启动/停止/查看采集器
.\scripts\observability\start-observability.ps1
.\scripts\observability\stop-observability.ps1
.\scripts\observability\status-observability.ps1

# 查询并手工触发一次任务
Get-ScheduledTask -TaskName SmartPropertyPublicDemoObservability
Start-ScheduledTask -TaskName SmartPropertyPublicDemoObservability

# 禁用或重新启用（不删除定义）
Disable-ScheduledTask -TaskName SmartPropertyPublicDemoObservability
Enable-ScheduledTask -TaskName SmartPropertyPublicDemoObservability

# 删除任务定义
.\scripts\observability\install-auto-start-task.ps1 -Remove
```

下次正常重启 Windows 后，不需要再次登录任务管理器或重新运行脚本。登录后等待 Docker Desktop 正常就绪约 1–3 分钟，然后依次检查：

1. `Get-ScheduledTask -TaskName SmartPropertyPublicDemoObservability`
2. `.\scripts\observability\status-observability.ps1`
3. 最新 `agent-lifecycle-YYYYMMDD.log` 中的 `WAITING_FOR_DOCKER`（如有）、`DOCKER_READY` 和 `COLLECTOR_START/ALREADY_RUNNING`
4. `ops\metrics` 与 `ops\docker-events` 的最新写入时间
5. `docker compose --env-file .env.docker ps` 中五项服务状态
6. 浏览器打开 `http://127.0.0.1:18080`
