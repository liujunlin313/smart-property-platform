# Backend

Phase 2A 后端已经可运行，技术栈为 Spring Boot 3.5、JDK 21、Spring Security、MyBatis、Flyway、MySQL 与 MinIO Java SDK。

主要接口：

- `/api/v1/auth/login|refresh|logout|me`
- `/api/v1/communities`
- `/api/v1/buildings`
- `/api/v1/building-units`
- `/api/v1/properties`
- `/api/v1/properties/{id}/floor-plan`
- `/api/v1/dashboard/summary`

所有查询需要登录；写接口只允许 `SYSTEM_ADMIN`。`DEMO_VIEWER` 可查询但不能写。数据库迁移只从 `../database/migrations/` 打包，不维护第二份 DDL。

日常开发从项目根目录执行 `deploy\start-backend-dev.ps1`，它使用 `mvn spring-boot:run` 直接编译和运行这里的源码；前端在 5173 单独运行。

生产前端只在最终构建时从 `../frontend/dist/` 打入可执行 JAR，同源 JAR 用于最终演示或临时公网部署，不是日常开发入口。

第一次阅读代码请从 [后端代码阅读指南](CODE_READING_GUIDE.md) 开始。
