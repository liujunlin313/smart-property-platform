# Stage B 核心业务开发报告

## 1. 本轮范围与保护边界

- 工程目标：`C:\Projects\数据库课程设计\多小区智慧物业管理系统_MERGED_V1`。
- 未修改：PRE_STAGE1 恢复工程、CURRENT_STAGE1 工程、现有 MySQL Schema 与业务数据。
- 数据库使用方式：仅执行读查询核对真实表结构和统计口径；未执行建表、迁移、清空、初始化或写入测试数据。
- 本轮未涉及：登录/JWT/Spring Security、Redis/MinIO 高可用行为、地图、居民端/中介端写操作、数据库 Schema 变更。
- 明确保留：`/assets` 原资产工作台及“小区 → 楼栋 → 单元”层级树；本轮没有向 `/properties` 增加第二棵树。

## 2. 真实数据库核对结果

本轮在 `community_db` 上只读核对，确认以下既有数据可支撑 Stage B：

| 项目 | 核对结果 |
| --- | --- |
| 当前有效居住关系 | 592 条 |
| 挂牌记录 | 80 条 |
| 产权单元 | 528 套 |
| 授权记录 | 64 条 |
| 2026 年账单 / 支付 | 3696 / 3324 条 |
| `rental_listing.status` | `PUBLISHED`、`RENTED`、`CLOSED`、`EXPIRED` |
| 当前有效产权 | 每套房产恰有一位当前产权人 |

已按确认口径实现：

- “已出租历史” = `rental_listing.status = RENTED`。
- Dashboard “本期已出租挂牌” = `status = RENTED` 且 `closed_at` 在所选年份内；`closed_at` 仅解释为成功出租结束挂牌的时间，不等同正式合同签约时间。
- 没有新增 `rented_at`、挂牌历史表或任何状态历史字段。

本轮没有发现阻塞 MVP 的数据库设计问题，因此未创建 `docs/DATABASE_ISSUES.md`。

## 3. 新增接口契约

所有接口保留既有统一响应封装，成功业务数据在 `data`，分页数据为既有 `PagedResponse`。全部通过既有 `/api/admin/**` 的 `SYSTEM_ADMIN` 边界访问。

### 3.1 居民管理

| Method | URI | 功能 | 主要查询参数 |
| --- | --- | --- |
| GET | `/api/admin/residents` | 当前有效居住人真实分页 | `page`、`pageSize`、`keyword`、`communityId`、`occupancyStatus`、`residentRole`、`relationType` |
| GET | `/api/admin/residents/summary` | 居住概览及分布 | 无 |
| GET | `/api/admin/residents/{residenceId}` | 居住关系详情 | 路径 ID |

关键词按姓名、手机号、房号做 contains 查询；列表手机号做脱敏；责任人、配偶、子女、合租人等关系由真实 `residence` 数据返回，不把 `ROOMMATE` 丢弃。

实现链：`ResidentController → ResidentService → ResidentServiceImpl → ResidentMapper → mapper/stageb/ResidentMapper.xml`。

### 3.2 租赁与中介

| Method | URI | 功能 | 主要查询参数 |
| --- | --- | --- |
| GET | `/api/admin/rentals` | 挂牌真实分页与复合筛选 | `page`、`pageSize`、`keyword`、`communityId`、`agencyId`、`status`、`listingMode`、`layoutType`、`rentMin`、`rentMax` |
| GET | `/api/admin/rentals/summary` | 挂牌、出租与平均值概览 | `year` |
| GET | `/api/admin/rentals/agencies` | 中介下拉选项 | 无 |
| GET | `/api/admin/rentals/{rentalListingId}` | 挂牌详情 | 路径 ID |

实现链：`RentalController → RentalService → RentalServiceImpl → RentalMapper → mapper/stageb/RentalMapper.xml`。列表与详情复用现有房产、小区、楼栋、单元、户型、授权和中介关系，不新增租赁表。

### 3.3 物业收费中心

| Method | URI | 功能 | 主要查询参数 |
| --- | --- | --- |
| GET | `/api/admin/fees/standards` | 收费标准 | `communityId`、`status` |
| GET | `/api/admin/fees/payments` | 支付流水真实分页 | `page`、`pageSize`、`communityId`、`feeTypeId`、`paymentMethod`、`status`、`year`、`month` |
| GET | `/api/admin/fees/overdue` | 欠费概览 | `year`、`month` |
| GET | `/api/admin/fees/overdue/list` | 欠费账单真实分页 | `page`、`pageSize`、`year`、`month` |

实现链：`FeeCenterController → FeeCenterService → FeeCenterServiceImpl → FeeCenterMapper → mapper/stageb/FeeCenterMapper.xml`。本轮没有将支付和账单写操作塞入管理页面。

### 3.4 Dashboard 与统计下钻

| Method | URI | 功能 |
| --- | --- | --- |
| GET | `/api/admin/dashboard/residence` | 居住概览、关系分布、小区分布、入住状态 |
| GET | `/api/admin/dashboard/rentals` | 挂牌/出租概览及小区、方式、中介、状态分布 |
| GET | `/api/admin/statistics/monthly/communities` | 指定年月的全部小区统计 |
| GET | `/api/admin/statistics/monthly/drilldown` | 指定年月、可选指定小区的统计与收费构成 |

统计页面交互固定为：**年度 → 月度 → 某月全部小区 → 某一个小区 → 明细**。明细账单复用既有账单分页接口，不复制第二套账单查询逻辑。

## 4. 前端页面与路由

| 页面/路由 | 本轮结果 |
| --- | --- |
| `/residents` | 已由占位页替换为真实居民管理页面：KPI、关系/入住图表、筛选、分页、详情抽屉。 |
| `/rentals` | 已由占位页替换为真实租赁页：状态、来源、小区、中介、户型、租金区间、关键词筛选，分页与详情。 |
| `/fees` | 新增收费中心入口：收费标准、账单管理、支付流水、欠费概览四个标签页。 |
| `/properties` | 保持表格/contains 搜索/筛选/真实分页；补充 `AVAILABLE_FOR_RENT` 展示色和 20/50/100 分页。没有树。 |
| `/dashboard` | 现有资产 Dashboard 增加真实居民与租赁 KPI、ECharts 图表；不使用随机数或硬编码业务统计。 |
| `/statistics/annual`、`/statistics/monthly` | 已实现年度到月度到小区再到明细的 URL 可回退下钻。 |
| `/assets` | 保持原已验收资产工作台及层级树，不修改其业务逻辑。 |

`frontend/src/router/index.js` 仅将原先占位路由指向新页面，并保留既有兼容路由；`frontend/src/layouts/AppLayout.vue` 只将导航入口改为居民、租赁与收费中心，未重做既有 UI 壳。

## 5. 版本审计矩阵

### ADD：新增业务文件

| 模块 | 文件组 | 职责 / 调用关系 |
| --- | --- | --- |
| 居民 | `resident/query/ResidentQuery`、`resident/vo/*`、`ResidentMapper`、`ResidentService`、`ResidentServiceImpl`、`ResidentController`、`ResidentMapper.xml` | 独立实现居民查询契约；Controller 只依赖 Service 接口，复杂 SQL 在 XML。 |
| 租赁 | `rental/query/RentalQuery`、`rental/vo/*`、`RentalMapper`、`RentalService`、`RentalServiceImpl`、`RentalController`、`RentalMapper.xml` | 独立实现挂牌分页、详情、概览和中介选项。 |
| 收费 | `billing/fee/query/*`、`billing/fee/vo/*`、`FeeCenterMapper`、`FeeCenterService`、`FeeCenterServiceImpl`、`FeeCenterController`、`FeeCenterMapper.xml` | 独立实现收费标准、支付流水与欠费查询。 |
| Dashboard | `DashboardDistributionVO`、`ResidenceDashboardVO`、`RentalDashboardVO` | 给既有 Dashboard 提供结构化真实统计，不向前端暴露 Entity。 |
| 统计 | `CommunityMonthlyVO`、`MonthlyDrilldownVO` | 支撑年度/月度/小区下钻的独立 VO。 |
| 前端 API/页面 | `api/fee.js`、`views/billing/FeeCenterView.vue` | 收费接口薄封装与组合式收费中心页面。 |

### MODIFY：已有文件的精确职责变化

| 文件 | 范围 | 修改前 → 修改后 | 原因 / 上下游 / 回归控制 |
| --- | --- | --- | --- |
| `mapper/stage1/PropertyMapper.xml` | `selectCurrentResidents` | 仅偏向旧角色显示 → 显示责任人/成员，并映射配偶、子女、合租人等关系与脱敏手机号 | 被既有房产详情调用；只改当前住户展示 SQL，不改资产树和房产主查询。 |
| `views/property/PropertyView.vue` | 全文件（16 行压缩组件） | 固定页大小和旧状态色 → 支持 20/50/100、出租可用状态与更清晰的 contains 文案 | 调用原 Property API；没有新增层级树或改变接口。 |
| `dashboard/mapper/DashboardMapper.java`、`DashboardMapper.xml` | 新增 Dashboard 查询声明/SQL | 仅资产概览 → 资产 + 居住 + 挂牌/出租真实统计 | 被 `DashboardServiceImpl` 调用；SQL 留在 XML，保留旧资产统计。 |
| `dashboard/service/*`、`DashboardController.java` | 新增 2 个只读端点 | 仅原 Dashboard 返回 → 增加 `/residence`、`/rentals` 数据 | Controller 仍调用接口；未变更旧端点。 |
| `statistics/mapper/*`、`StatisticsService*`、`StatisticsController.java` | 新增下钻查询 | 年月汇总 → 可进入小区与单小区收费构成 | 保留旧统计接口；使用既有账单事实表。 |
| `api/dashboard.js`、`api/statistics.js` | 新增 API 调用函数 | 无居民/租赁/下钻客户端 → 对应新增后端 GET 调用 | 继续使用既有 `http.js`，未重写认证/拦截器。 |
| `views/dashboard/DashboardView.vue` | KPI/图表区域 | 仅资产指标 → 追加居民、挂牌与“本期已出租挂牌”真实图表 | 数据来自 Dashboard API；未使用 `Math.random()`。 |
| `views/rental/RentalView.vue` | 全文件（21 行压缩组件） | 占位 → 真实租赁 KPI、图表、筛选、分页、详情 | 由 `/rentals` 路由调用；包含户型筛选。 |
| `views/statistics/AnnualStatisticsView.vue`、`MonthlyStatisticsView.vue` | 全文件（17 / 25 行压缩组件） | 占位 → 真实年度/月份/小区/明细下钻 | 通过 URL 查询参数保存下钻状态，支持返回。 |
| `router/index.js` | 路由定义（33 行） | 三个占位路由 → 指向真实页面 | 保留 `/assets`、`/map`、`/bills` 等兼容路由。 |
| `layouts/AppLayout.vue` | 导航数组（88 行） | 占位标签 → 居民、租赁、收费中心标签 | 未删除原资产工作台。 |
| `logback-spring.xml` | Console pattern | 默认 INFO 行携带 `trace=...` → Console 不逐行展示 trace | `X-Trace-Id`/MDC 机制与文件日志信息仍保留。 |

### KEEP：明确未动的高风险文件

| 文件 | 保持原因 |
| --- | --- |
| `SmartPropertyApplication.java` | 不重做启动、PID、基础设施准备和启动摘要。 |
| `config/SecurityConfig.java` | 不改变既有 JWT/Security、`SYSTEM_ADMIN` 管理端边界。 |
| `resources/application.yml` | 不改变数据库、Redis、MinIO 或端口配置。 |
| `frontend/src/api/http.js` | 不改变 `/api/v1` 与 `/api/admin` 的既有认证/错误处理链。 |
| `frontend/src/main.js` | 不改变前端启动、监控或全局依赖装载方式。 |

## 6. 代码结构与 DTO/VO 边界

- Controller 依赖 Service Interface，不直接依赖 `*ServiceImpl`。
- Query 只接收列表筛选/分页参数；Entity 没有直接作为 API 响应返回；VO 只输出页面需要的数据。
- 复杂 JOIN、统计、GROUP BY、条件分支都位于 MyBatis XML。
- 新模块命名使用 `resident`、`rental`、`billing.fee`，不把所有 Controller、Mapper、DTO 混放到一个包。

## 7. 本轮验证结果

| 检查项 | 结果 | 证据 / 说明 |
| --- | --- | --- |
| 后端编译 | PASS | `mvn -q -DskipTests compile` 完成，无编译错误。 |
| 后端打包 | PASS | `mvn -q -DskipTests package` 完成。 |
| 前端构建 | PASS | 最后一次 `npm run build` 成功，2294 个模块完成转换；仅有既有依赖 PURE 注释与 ECharts 体积警告。 |
| MyBatis/MySQL 真实只读核对 | PASS | 已读取 `community_db` 的表结构、记录量、枚举分布和统计数据。 |
| 新建后端进程 / 新 API HTTP Smoke | NOT RUN | 8080 被已有 Java 进程 PID 17192 占用；启动预检按既有约定 fail-fast，未杀进程、未换端口。 |
| Redis / MinIO 新进程链路 | NOT RUN | 避免为绕过现有 8080 进程而启动第二套运行链。 |
| 登录/JWT 回归 | NOT RUN | 需要在端口空闲后由同一运行链执行；本轮未破坏认证文件。 |
| 浏览器自动化 | 未执行（按要求） | 未使用 Playwright、Selenium 或批量点击。 |

## 8. Deferred / 后续项目

1. 完整租赁生命周期：正式合同、出租时间、挂牌状态历史与合同确认链路；本轮严格采用现有 `RENTED + closed_at` 口径。
2. 全角色 RBAC：`PROPERTY_STAFF`、`RESIDENT`、`AGENCY_STAFF` 及对象级权限；本轮继续只有管理员端。
3. 收费写操作、对账、撤销、支付回调与并发幂等控制。
4. 地图/空间检索、高德 API 与附近房源功能。
5. 居民与中介侧独立页面、授权申请/确认的完整交互。
6. ECharts 产物分包优化；当前 Vite 仅给出体积警告，构建仍成功。
7. 端口 8080 空闲后，执行一次新构建的启动、管理员登录及新增接口 HTTP Smoke，再补充运行期结果。

## 9. 人工验收建议

待当前占用 8080 的旧 Java 进程自然停止或由操作者确认停止后，只启动 MERGED_V1 的既有运行链，然后以管理员登录验证：

1. Dashboard 的居民、租赁 KPI 和图表均显示真实数值。
2. `/residents` 的关键词、关系筛选、分页和详情。
3. `/rentals` 的户型/中介/状态/租金筛选和详情。
4. `/fees` 四个标签页与账单管理入口。
5. 年度统计点击月份后，按“全部小区 → 单小区 → 明细”继续下钻并返回。
