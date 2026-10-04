# 多小区智慧物业管理系统

这是一个可长期演进的课程大项目。当前正在完成 **Phase 2A 稳定化**：Vue、Spring Boot、MyBatis、MySQL、JWT、Redis 降级链路和 MinIO 已形成真实本地链路。

## 当前已经完成

- 系统管理员与只读游客登录；短期 JWT + HttpOnly Refresh Cookie 轮换。
- `Community → Building → BuildingUnit → PropertyUnit` 四级资产档案。
- 小区和楼栋经纬度、状态生命周期、草稿房产、乐观锁和变更审计。
- 面积为 `NULL` 时房产强制为 `DRAFT`，页面显示“未填”。
- 一套房只允许一个产权人；产权转移通过历史区间保留，不允许当前多人共有。
- 户型枚举仍保留；户型图上传针对整套 `PropertyUnit`，不引入 `Room`。
- MinIO 私有桶上传与鉴权下载；只接受真实 JPEG/PNG/WebP 文件头。
- Vue 管理工作台、层级树、筛选、分页、新增/编辑抽屉和游客只读控制。
- 日常开发严格分离：Spring Boot 只提供 8080 API，Vue/Vite 只提供 5173 页面；最终合包留到功能完成后再设计。
- 所有业务 JSON 统一为 `success/code/message/data/status/traceId/timestamp`；普通对象和分页对象都放入 `data`。

## 目录

```text
backend/                 Spring Boot 3.5 + JDK 21 + MyBatis
frontend/                Vue 3 + Vite + Element Plus
database/migrations/     已冻结的 V1～V3 建库基线
database/scripts/        Phase 1 与 Phase 2A 自动验证
database/evidence*/      实际回归日志
deploy/                  本地构建、启动、依赖检查与精确 PID 回收脚本
docs/                    设计与交付说明
```

## 日常开发入口

推荐的日常开发方式是：IDEA 只运行后端，WebStorm 只运行前端：

```powershell
# IDEA：运行 SmartPropertyApplication
# WebStorm 内置终端（frontend 目录）：
npm run dev
```

如果希望在一个 PowerShell 窗口观察两端日志，可显式运行 `.\deploy\start-dev.ps1 -SkipInstall`。这只是辅助入口，不替代 IDEA/WebStorm 分头开发。

开发页面为 `http://127.0.0.1:5173`。完整首次配置、数据库含义和代码阅读顺序见 [从这里开始](START_HERE.md) 与 [后端代码阅读指南](backend/CODE_READING_GUIDE.md)。

Flyway 自动迁移已经冻结，日常启动和本轮回归只使用同一个 `community_db`。旧的隔离库验证脚本仅保留为历史证据，不再是当前开发入口。

本轮工程稳定化结论与实测证据见 [Engineering Stabilization V2](ENGINEERING_STABILIZATION_V2.md)。最终打包方案已暂缓，当前默认 Maven 构建不会复制 `frontend/dist`。

## 后续阶段

- Phase 2B：个人用户、中介管理员、对象级权限、授权申请、合同确认、消息抄送和出租状态机。
- Phase 2C：高德地图真实点位、附近租房、空间索引和空间范围查询。
- 后续业务域：物业收费、车辆与车位、家具二维覆盖层编辑器及查询优化案例。

本轮不会把某一条复杂 SQL 包装成“唯一核心”。费用统计、模糊搜索、空间查询、复合筛选、窗口函数和执行计划分析会分别在适合的业务模块中实现。
