# 从这里开始：日常开发模式

项目根目录：

```text
C:\Projects\数据库课程设计\多小区智慧物业管理系统
```

## 1. 源码与 JAR 的关系

日常开发不使用已经打包的 JAR。开发时分别运行：

```text
MySQL 3306
MinIO 9000 / 9001
Spring Boot 源码开发进程 8080
Vue/Vite 源码开发进程 5173
```

`backend\target\*.jar` 只是一次生产构建验证结果，可以删除，也可以在最终演示或部署时重新生成，不是源码容器。

后端使用 IntelliJ IDEA。Vue 推荐使用 JetBrains WebStorm：它是完整的前端 IDE，支持 Vue、Vite、TypeScript/JavaScript、npm、调试和浏览器联调，不需要 VS Code。可以分别打开 `backend` 与 `frontend`，两边互不负责启动对方。

真正需要阅读和修改的是：

```text
backend\src\main\java       Java 源码
backend\src\main\resources  MyBatis XML 与后端配置
frontend\src                Vue 源码
database\migrations         已冻结的 V1～V3 建库基线（启动时不自动执行）
```

后端阅读顺序见 `backend\CODE_READING_GUIDE.md`。

## 2. 本机当前状态

- MySQL 8 已安装并作为 `MySQL80` 服务运行在 `127.0.0.1:3306`。
- MinIO 已安装在 `C:\Program Files\minio\minio.exe`，项目脚本会优先复用它，不会重新下载。
- 日常开发和本轮回归都只使用 `community_db`，不创建临时数据库。
- Redis 是性能层；不可用时后端明确进入 `CACHE BYPASS`，业务仍直接访问 MySQL。

## 3. “准备数据库”具体指什么

不是重新安装 MySQL，也不是每次启动都重建库。当前 `community_db` 已经是唯一开发数据库，Spring Boot 只连接并使用它。

在 MySQL Workbench 中用管理员连接执行一次：

```sql
CREATE DATABASE IF NOT EXISTS community_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'smart_property_app'@'localhost'
  IDENTIFIED BY '请换成只用于本项目的开发密码';

CREATE USER IF NOT EXISTS 'smart_property_app'@'127.0.0.1'
  IDENTIFIED BY '请换成只用于本项目的开发密码';

GRANT ALL PRIVILEGES ON community_db.*
  TO 'smart_property_app'@'localhost';

GRANT ALL PRIVILEGES ON community_db.*
  TO 'smart_property_app'@'127.0.0.1';

FLUSH PRIVILEGES;
```

如果数据库或账号已存在，不要重复创建，只需确认连接信息与权限。

V1～V3 只作为已完成的建库基线保留。`APP_FLYWAY_ENABLED=false`，后端启动不会自动建新库或修改表。以后需要变更结构时，先保存一份可审查 SQL，再明确执行到同一个 `community_db`。

## 4. 创建开发环境配置

```powershell
Set-Location 'C:\Projects\数据库课程设计\多小区智慧物业管理系统'
Copy-Item .\deploy\local.env.example.properties .\deploy\local.env.properties
notepad .\deploy\local.env.properties
```

这是 PowerShell 和 IDEA Java Main 共用的本地配置。如果已经有 `local.env.ps1`，首次启动会自动转换，原文件保留且不会打印密码。

## 5. 日常开发的两个窗口

### 自动运行：MySQL

`MySQL80` 是自动启动的 Windows 服务，一般不需要另开窗口。Workbench 用于查看表和数据。

### 窗口一：IDEA 中的 Spring Boot 后端

打开 `backend` Maven 模块，直接运行 `SmartPropertyApplication.main()`。Java 会读取 `deploy\local.env.properties`，检查唯一的 `community_db`，并复用或启动 Redis/MinIO；点击 IDEA 红色停止按钮后，只回收本次精确 PID。

不使用 IDEA 时，等价命令仍是 `.\deploy\start-backend-dev.ps1`。

后端地址：`http://127.0.0.1:8080`

### 窗口二：WebStorm 中的 Vue 前端

```powershell
Set-Location 'C:\Projects\数据库课程设计\多小区智慧物业管理系统\frontend'
npm run dev
```

WebStorm 的 npm 工具窗口也可以直接双击 `dev`。它运行 Vite、热更新 Vue/CSS，并把 `/api` 转发到后端 8080。关闭 WebStorm 的 `dev` 任务不会停止 IDEA 后端。

开发页面：`http://127.0.0.1:5173`

### 4.4 干净重启或解决端口占用

若 8080/5173 已经有本项目的旧开发进程，新的启动命令会直接显示旧 PID，不会重复启动。先在项目根目录运行：

```powershell
.\deploy\stop-development.ps1
```

看到 `Project development services are stopped. MySQL was not touched.` 后，再分别启动后端和前端。停止脚本按 PID、可执行文件路径和启动时间精确核验，不会按名称结束其他 Java/Node 程序。

### 可选：一个终端联合启动

只在需要集中观察日志时使用：

```powershell
.\deploy\start-dev.ps1 -SkipInstall
```

Ctrl+C 会按 PID、可执行文件和启动时间精确回收该次启动创建的 Java、Node、Redis、MinIO；不会结束外部复用进程。

## 6. 什么时候才打包

现在不打包。当前 Maven 构建不复制 `frontend/dist`，8080 根路径返回 404，唯一开发页面是 5173。等 Phase 2B/2C 业务完成并通过联调后，再单独确定最终演示合包方式。

查看当前端口和归属：

```powershell
.\deploy\status-dev.ps1
```
