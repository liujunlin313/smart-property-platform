# Phase 2A 稳定化测试检查点

日期：2026-08-22  
项目目录：`C:\Projects\数据库课程设计\多小区智慧物业管理系统`

## 数据库边界

- 唯一开发数据库：`community_db`。
- 已确认冻结基线：V3。
- `APP_FLYWAY_ENABLED=false`，应用启动没有执行迁移。
- 本轮没有创建临时数据库，没有导入演示业务数据，没有修改唯一约束、外键或 CHECK 约束。
- 旧隔离库脚本已冻结；当前入口为 `database\scripts\verify-current-development.ps1`。

## 已通过

- 后端 JDK 21 编译与完整 JAR 打包：PASS。
- Vue/Vite 生产构建：PASS。
- MySQL `community_db` 连接与资产统计读取：PASS。
- 管理员 JWT 登录、Refresh Token 轮换：PASS。
- 连续 3 次错误密码均返回 401，随后正确密码立即登录成功；不存在 15 分钟持久锁定：PASS。
- 只读游客查询：PASS；尝试写入返回 HTTP 403。
- Redis 正常链：`PING=PONG`、心跳 `cacheMode=READY`。
- Redis 缺失降级链：应用正常启动、`cacheMode=BYPASS`、MySQL 查询仍成功。
- MinIO 私有桶健康检查：PASS。
- Microsoft Edge 登录与 `/assets`、`/people`、`/rentals`、`/fees`、`/map`、`/settings`、`/notifications` 路由：PASS。
- 前端动作批量上报：Edge 回归观察到 17 条 `[UI ACTION]` 日志。
- 后端停止连续三次心跳失败：自动跳转 `/login?offline=1&redirect=/assets`，PASS。
- 精确 PID 回收：项目启动的 MinIO PID 19784 已停止，托管记录数归零；未执行按名称全局结束进程。

## 证据

- `database\evidence\phase2a-stabilization\edge-assets-page.png`
- `database\evidence\phase2a-stabilization\edge-backend-offline.png`
- 可重复命令：`database\scripts\verify-current-development.ps1`

## 当前状态

Phase 2A 技术稳定化通过。Phase 2B 业务表、账号与“房东申请中介接单”流程尚未开始，本轮没有越界开发。
