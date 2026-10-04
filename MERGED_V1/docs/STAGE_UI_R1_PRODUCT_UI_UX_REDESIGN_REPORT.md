# Stage UI-R1 产品化 UI/UX 重构报告

## 1. 范围与安全边界

本轮仅改动 `_MERGED_V1/frontend` 的页面组织、样式与展示层，并新增 UI 文档。未修改后端 Java、MyBatis、数据库 Schema/数据、JWT/Spring Security、角色权限、DataScope、Redis、MinIO、高德配置或任何 API 路径。

执行前先依据 Vue Router、Layout、视图和 API 模块建立了 Before 功能清单；执行后按该清单复核，完整清单见 `STAGE_UI_R1_FUNCTION_INVENTORY.md`。

## 2. 已完成的产品化调整

### 全局与信息架构

- 建立统一的设计 token 与表格、卡片、标签、窄屏基础样式。
- 新增 `PageHeader`、`SectionCard`、`StatCard`、`StatusTag`、`EmptyState` 五个复用组件。
- 将状态/枚举的中文展示与颜色语义集中在 `src/constants/presentation.js`，避免页面直接暴露业务码。
- 管理端导航重组为六个物业业务分区；保留原 14 个入口及基于角色的可见性。
- 保留 `PROPERTY_STAFF` 的后端数据范围表达，未在前端读取全量数据后过滤。

### 管理端高频操作

- 数据总览：改为真实统计接口驱动的业务总览、趋势、收费状态、住户结构、逾期与挂牌信息。
- 房产管理：加入页面语境、统一筛选区、空态、分页表达和中文状态；详情抽屉保留产权人、居住人、计量表。
- 入住办理：整理为“选择人员 → 填写入住信息”双步骤操作面，保留创建人员、可入住房产查询与提交链路。
- 居民异常：补齐真实状态数量、事项表、中文状态和处理动作。
- 账单明细：统一筛选、金额展示、状态标签、账单项目和支付记录详情。
- 原资产工作台、收费中心、通知、租赁、统计与地图均继续使用其既有真实 API；未改动其业务动作或地图链路。

### 居民端

- 重新组织为清晰的居住、账单、通知和服务入口。
- 保留居所、产权、账单、通知、异常记录与异常上报六条原 API 链路。
- 增加更有意义的空态与账单状态表达；不创建任何管理端入口或越权查询。

## 3. 关键文件

| 类型 | 文件 |
| --- | --- |
| 管理端分组导航 | `frontend/src/layouts/AppLayout.vue` |
| 居民端布局 | `frontend/src/layouts/ResidentLayout.vue` |
| 统一展示层 | `frontend/src/components/common/`、`frontend/src/constants/presentation.js` |
| 总览 | `frontend/src/views/dashboard/DashboardView.vue` |
| 房产 / 账单 / 入住 / 异常 | `frontend/src/views/property/PropertyView.vue`、`billing/BillView.vue`、`residency/MoveInView.vue`、`incident/IncidentView.vue` |
| 居民门户 | `frontend/src/views/residentportal/ResidentPortalView.vue` |

## 4. 验证结果

| 项目 | 结果 | 说明 |
| --- | --- | --- |
| 功能清单核对 | PASS | 管理端 14 路由、居民端 6 路由均在。 |
| Vue 生产构建 | PASS | `npm run build` 成功；未启动开发服务器。 |
| 后端编译 / HTTP Smoke | 未执行（不适用） | 本轮没有改动后端，且没有占用用户的 8080/5173。 |
| 浏览器人工验收 | 待用户验收 | 请在自己的前端 IDE 启动后按角色和路由做最终交互确认。 |

构建输出存在既有的 ECharts 大包体积提示，以及第三方依赖 `@vueuse/core` 的注释提示；两者均不影响本轮编译结果，未为 UI-R1 擅自调整构建拆包策略。

## 5. Deferred

- 表格数据导出、批量导入/导出：现有 API/业务规则未纳入本轮，未制作假按钮。
- 逐页浏览器人工验收：按要求留给用户在真实登录身份下完成；本轮未启动或占用用户端口。
- 更深层的数据可视化拆包：ECharts 已按视图懒加载，但公共依赖包仍较大；应作为独立性能任务评估。
