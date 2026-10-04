# 开发、构建与演示入口

## 日常开发（默认工作流）

默认分头开发：

```powershell
# IDEA 运行 SmartPropertyApplication
# WebStorm 的 frontend 目录运行
npm run dev
```

脚本等价入口仍是 `start-backend-dev.ps1` 和 `start-frontend-dev.ps1`。需要单终端聚合日志时才运行 `start-dev.ps1 -SkipInstall`。

如果端口已经由本项目占用，启动器会明确显示旧进程 PID，而不会再启动第二份。需要完全重启时先运行：

```powershell
.\deploy\stop-development.ps1
```

该脚本只停止经过页面/心跳确认且 PID 身份匹配的本项目前端、后端、Redis 和 MinIO；不会停止 MySQL，也不会按进程名批量结束程序。

MySQL 使用本机正在运行的 `MySQL80` 服务。前端访问 `http://127.0.0.1:5173`，Vite 代理后端 `8080`。

后端 Java Main 会复用或启动本项目的 Redis/MinIO，并在退出时只回收本次精确 PID。Redis 不可用时进入 `CACHE BYPASS`；MinIO 不可用时只有文件接口返回 503。

## 数据库边界

开发、启动和回归都使用现有 `community_db`。Flyway 自动执行已冻结，启动不会创建临时库或自动改表。V1～V3 迁移文件仅保留为已完成基线。

## 最终构建与演示

本轮不执行最终合包。默认后端构建不包含 `frontend/dist`，8080 只提供 API，开发页面固定为 5173。最终演示入口等业务稳定后再单独设计和回归。

公网演示与额外安全加固不属于本轮 Phase 2A 本地开发范围。
