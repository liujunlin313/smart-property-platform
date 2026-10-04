# Stage B 前端交互与分页能力审计

**审计性质：只读。** 本报告基于 `MERGED_V1` 当前源码与已有访问日志完成。未修改源码、配置、数据库、Flyway、Redis、MinIO、JWT 或启动脚本；未启动/停止服务，未执行写库接口。

## 一、结论摘要

1. **后端分页并非缺失。** `/residents`、`/rentals`、`/properties`、`/bills`、收费标准、支付流水、欠费列表都执行 `COUNT + LIMIT/OFFSET`，并返回真实分页对象。
2. **分页“只看到第一页”的核心是前端可用性，而非 SQL。** 页面都把 `el-pagination` 放在表格之后，表格没有固定高度/内部滚动；20 行长表在普通桌面视口中会把分页推到页面很下方。收费子页面还没有总数、每页条数和跳页功能；账单没有页大小选择。
3. `/properties` 的搜索代码链静态检查完整：`v-model → q.keyword → getProperties(q) → Controller Query → MyBatis LIKE` 均已接通。已有日志只记录了空关键词初始请求，**没有找到一次非空关键词请求**，所以不能从证据上判定“搜索失效在后端”；实际体验问题需要下一轮用非空关键词 GET 或浏览器 Network 验证。
4. `/rentals` 当前把 KPI、三张图和长表混在同一连续页面；没有表格高度约束，故必须滚整个页面才能到分页。它还直接显示了 `TWO_BEDROOM_ONE_LIVING` 等枚举编码，且页面 KPI 用的是“已出租历史”而不是已实现但未使用的“本期已出租挂牌”。
5. 账单详情接口**已存在**，并返回账单项目、计量读数和支付记录；但列表与统计明细只展示 `propertyNo`，缺少完整位置，UI 也没有完整呈现账期、详细位置、实收、截止日、状态、读数/单位等内容。
6. 已有统计是时间维度下钻：`年度 → 月度 → 小区 → 账单`。费用类别维度只做到汇总展示，尚无 `fee_type → community → bill_item` 的可点击下钻。
7. 已有日志确认 residents、rentals、bills、收费标准、支付、欠费、properties 在数十毫秒内有重复请求。源码明确可证的重复点是 `MonthlyStatisticsView.vue` 同时使用路由 `watch` 与 `onMounted`；其它页面在单个 View 源码中只有一次 `onMounted`，因此更可能是运行时发生了双实例/重挂载，不能将根因误写成不存在的双 watch。
8. `ResizeObserver loop completed with undelivered notifications` 有日志证据，`EChart.vue` 是唯一源码中直接创建 `ResizeObserver` 的组件；但出错路由为 `/fees`、`/properties`，这两个页面本身没有 EChart，因此当前无法证明它就是唯一根因。不能直接过滤错误，下一轮应先添加安全的来源标记/最小复现。

## 二、分页基础契约

### 2.1 当前返回对象

管理员业务接口使用：

```java
PageResult<T> {
  records: List<T>,
  total: long,
  page: int,
  pageSize: int
}
```

接口响应仍由既有统一外层封装承载；`frontend/src/api/http.js` 的响应拦截器会解包外层，将上述 `PageResult` 放到前端 API 函数收到的 `r.data`。

另有 `PagedResponse<T> { items, page, pageSize, total, totalPages }`，但它只被旧 `/api/v1` 资产模块使用；Stage B 管理端查询没有使用它。这是统一分页抽象前必须处理的双口径事实。

### 2.2 后端分页实现模式

所有 Stage B 管理端分页 Service 均采用：

```text
Controller 接收 Query
→ Service 标准化 page/pageSize（默认 1/20，上限 100）
→ Mapper.countXxx(query)
→ offset = (page - 1) * pageSize
→ Mapper.selectXxxPage(query, offset, limit)
→ PageResult.of(records, total, page, pageSize)
```

对应 MyBatis XML 使用独立 `count...` 与 `... LIMIT #{limit} OFFSET #{offset}`，而不是前端一次性拉全量。

## 三、逐页面矩阵

| 页面 | View / API | 后端 endpoint | 后端分页 | 前端分页现状 | 搜索/筛选 | 固定表高 | 详情能力 | 已发现问题 | 优先级 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 居民 | `views/resident/ResidentView.vue` / `api/resident.js` | `GET /admin/residents` | `countResidents` + `selectResidentPage LIMIT/OFFSET`；`PageResult.records,total,page,pageSize` | 有 `sizes, prev, pager, next`，可改 20/50/100；变页会 `load()` | 姓名/手机号/房号，及小区、入住、角色、关系 | 无 | `/admin/residents/{id}` 抽屉 | 控件在长表底部；日志确认初始列表+summary 双请求 | P0 / P1 |
| 租赁 | `views/rental/RentalView.vue` / `api/rental.js` | `GET /admin/rentals` | `countRentals` + `selectRentalPage LIMIT/OFFSET`；同上 | 有完整页大小/页码 | 小区/中介/状态/来源/户型/租金/关键词 | 无 | `/admin/rentals/{id}` 抽屉 | 看板和工作台混排；中文户型映射缺失；历史 KPI 文案错误；日志双请求 | P0 / P1 / P2 |
| 房产 | `views/property/PropertyView.vue` / `api/property.js` | `GET /admin/properties` | `countProperties` + `selectPropertyPage LIMIT/OFFSET`；同上 | 有 `sizes,prev,pager,next` | 房号/小区/楼栋/单元，及小区、入住状态 | 无 | `/admin/properties/{id}` 抽屉 | 静态链完整但需要对非空关键词做运行期确认；日志双请求；分页在长表后 | P0 / P1 |
| 账单 | `views/billing/BillView.vue` / `api/bill.js` | `GET /admin/bills` | `countBills` + `selectBillPage LIMIT/OFFSET`；同上 | 有 `prev,pager,next`；**未绑定 pageSize，固定 20** | 后端支持 `propertyKeyword`，前端没有输入框绑定它 | 无 | `/admin/bills/{id}` 有项目/支付详情 | 账单号首列权重过高；列表仅房号；过滤功能未完整暴露；日志双请求 | P0 / P1 |
| 收费标准 | `views/billing/FeeCenterView.vue` / `api/fee.js` | `GET /admin/fees/standards` | `countFeeStandards` + `selectFeeStandardPage LIMIT/OFFSET` | 有 `prev,pager,next`，无 total/sizes/jumper，固定 20 | 小区、状态 | 无 | 无（只读清单） | 组件样式与账单不统一；无法直观看总量/页大小 | P0 |
| 缴费记录 | 同上 | `GET /admin/fees/payments` | `countPayments` + `selectPaymentPage LIMIT/OFFSET` | 有 `prev,pager,next`，无 total/sizes/jumper，固定 20 | 小区、支付状态、方式；year/month 参数存在但 UI 未暴露 | 无 | 无 | 分页能力不完整；过滤只暴露一部分后端能力；日志双请求 | P0 / P1 |
| 欠费管理 | 同上 | `GET /admin/fees/overdue/list` | `countOverduePage` + `selectOverduePage LIMIT/OFFSET` | 有 `prev,pager,next`，无 total/sizes/jumper，固定 20 | 小区；year/month 参数存在但 UI 未暴露 | 无 | 无（账单号没有跳详情） | 列表分页不完整；页码在长内容底部；日志双请求 | P0 / P1 |
| 月度统计 | `views/statistics/MonthlyStatisticsView.vue` / `api/statistics.js` + `api/bill.js` | `GET /admin/statistics/monthly`、`/monthly/communities`、`/monthly/drilldown`，明细复用 `/admin/bills` | 月度/小区/汇总无分页；小区账单明细为 `PageResult` | 明细有 `prev,pager,next`，固定 20 | 路由 `year/month/communityId` | 无 | 仅账单列表，无 bill drawer | 路由 watch + onMounted 明确重复；明细只房号；无费用类别下钻 | P1 |
| 年度统计 | `views/statistics/AnnualStatisticsView.vue` / `api/statistics.js` | `GET /admin/statistics/annual` + `/monthly` | 年度及月度聚合本身不分页 | 无需分页 | 仅 year | 不适用 | 点击月进入月度 | “12 个月”标题与只返回 1–7 月数据矛盾；费用构成不可下钻 | P2 / P1 |

## 四、`/properties` 搜索全链路审计

### 4.1 前端到 SQL

```text
PropertyView.vue
  <el-input v-model="q.keyword" ...>
  查询按钮 / Enter：q.page = 1; load()
       ↓
  load(): getProperties(q)
       ↓
api/property.js
  http.get('/admin/properties', { params })
       ↓
PropertyController.pageProperties(PropertyQuery query)
       ↓
PropertyServiceImpl.normalize(query)
  trim keyword；随后 countProperties + selectPropertyPage
       ↓
PropertyMapper.xml / propertyWhere
  pu.property_no LIKE CONCAT('%', keyword, '%')
  OR c.community_name LIKE CONCAT('%', keyword, '%')
  OR b.building_name LIKE CONCAT('%', keyword, '%')
  OR bu.unit_name LIKE CONCAT('%', keyword, '%')
```

结论：这条静态链没有发现 `searchText` 与 `q.keyword` 脱节、点击未调用 `load()`、watch 覆盖关键词、API 未传 keyword 等问题。`page` 也会在查询前重置为 1。

日志只证明初始加载的 `keyword=` 是空；未发现实际输入后带非空 `keyword` 的访问记录。因此“用户输入后仍搜不出结果”应列为 **P0 运行期验证问题**，而不是在没有证据时误判 MyBatis 或 Controller 已坏。下一轮应在浏览器 Network 或同一登录会话发一个带非空关键词的 GET，检查 URL、响应 `total`、表格是否更新。

对比：居民与租赁的 keyword 链同样完整；账单后端有 `propertyKeyword`（房号或小区 LIKE），但 `BillView.vue` 没有对应输入框，属于已证实的前端功能缺口。

## 五、前端抽象现状与最小方案

当前不存在：

- `frontend/src/components/common/`；
- `PaginationBar.vue`；
- `frontend/src/composables/`；
- `usePagedQuery.js`；
- 可复用通用业务表格。

建议的最小抽象层级正确，且适合本工程：

1. 新增 `components/common/PaginationBar.vue`：统一显示“共 N 条”、`sizes, prev, pager, next, jumper`，接收 `total/page/pageSize/pageSizes`，发出 `page-change`、`page-size-change`。
2. 新增 `composables/usePagedQuery.js`：只统一 `page`、`pageSize`、`total`、`loading`、`resetToFirstPage`、分页回调；各 View 仍保留自己的筛选参数、列定义、Drawer 与详情加载。
3. 在实施时选择一种标准分页返回类型：管理端继续统一为 `PageResult.records,total,page,pageSize`；旧资产 `/api/v1` 的 `PagedResponse.items,totalPages` 保持兼容，不应在本轮硬改旧接口。

不建议抽象万能 `<BusinessTable>`：居民、租赁、账单、房产各自的列、操作和详情抽屉差异过大，反而会降低可维护性。

## 六、租赁页专项审计

### 6.1 为什么必须滚动整页

`RentalView.vue` 顺序固定为：6 个 KPI → 3 张 ECharts → 筛选栏 → 11 列表格 → 分页。CSS 只定义 `.pager`，没有为 `el-table` 设置 `height`、`max-height` 或外层 `overflow:auto`。因此表格 20 行会自然撑高文档，分页处于页面底部。

最小 UI 改造方向：保留顶部 KPI，以下改为“挂牌列表 / 数据分析”两个 Tab；挂牌列表中将筛选、固定高度的表格容器和分页作为单个工作台，数据分析 Tab 放方式/状态/小区/租金区间/户型/中介图表。该建议不要求改变租赁 API。

### 6.2 枚举与 KPI

- `RentalMapper.xml` 返回 `lt.layout_type`，前端 `<el-table-column prop="layoutType">` 原样展示，因此 `TWO_BEDROOM_ONE_LIVING` 会直接显示。
- 前端只定义了 `statusLabels` 和 `modeLabels`，缺少 `layoutTypeLabels`。
- 后端 `RentalSummaryVO` 同时有 `rentedHistoryCount` 与 `rentedInPeriodCount`：
  - `rentedHistoryCount = SUM(status = 'RENTED')`；
  - `rentedInPeriodCount = SUM(status = 'RENTED' AND YEAR(closed_at) = year)`。
- 租赁页卡片当前使用的是 **`rentedHistoryCount`**，文案为“已出租历史”；Dashboard 正确使用 `rentedInPeriodCount`，文案为“本期已出租挂牌”。两处语义不一致，属 P2。
- `selfCount`、`agencyCount` 的 SQL 均没有 status 限制，统计的是所有历史挂牌 80 条，不是当前 `PUBLISHED`。如果页面要表达“当前来源分布”，需要另加状态限定或修改文案。

## 七、收费与账单专项审计

### 7.1 账单详情并非不存在

已有：

```text
GET /api/admin/bills/{billId}
BillController → BillServiceImpl → BillMapper.xml
```

返回 `BillDetailVO`，包含：

- 账单头：`billNo`、小区、楼栋、单元、房号、年月、应收/实收/欠费、截止日、状态；
- `items`：费用名称、数量、单价、金额、上/本期读数、备注；
- `payments`：支付单号、付款人、金额、支付时间、方式、状态。

因此数据库/API 已能支持“账单项目 + 支付记录”。不过当前 Drawer 仅展示账单号、房号、应收、欠费；项目表未展示读数，支付表未展示支付方式/状态，未呈现完整位置、账期、实收、截止日和账单状态。它是**能力已有但前端呈现不完整**，不是没有 endpoint。

### 7.2 完整位置问题

- `BillMapper.selectBillPage` 只 select `community_name` 和 `property_no`；`BillListVO` 也没有楼栋/单元/完整位置字段。
- `BillView.vue` 因而只显示“小区 | 房产（房号）”。
- 月度统计明细复用 `GET /admin/bills`，也只显示 `propertyNo`。
- 支付流水与欠费列表已经使用 `fullLocation`，说明 Join 路径已在工程中验证可用。

这属于 P1 业务可用性问题：不同楼栋/单元同房号无法靠当前列表区分。账单号仍应保留在数据库、接口和详情中；UI 应降为次要定位信息，而非第一高权重列。

## 八、统计下钻审计

### 8.1 已实现的时间维度

```text
GET /admin/statistics/annual?year
GET /admin/statistics/monthly?year
  → 点击月份
GET /admin/statistics/monthly/communities?year&month
GET /admin/statistics/monthly/drilldown?year&month[&communityId]
  → 指定小区时复用 GET /admin/bills?year&month&communityId&page&pageSize
```

SQL 分别从 `v_monthly_collection_summary`、`v_bill_financial_summary`、`bill → bill_item → fee_standard → fee_type` 聚合。时间维度链路存在且日志中有成功响应。

### 8.2 缺少的费用类别维度

当前 `selectFeeComposition(year)` 与 `selectMonthlyFeeComposition(year, month, communityId)` 只返回按费用类别聚合的金额，前端费用构成表没有点击行为；Mapper/Controller 中没有按 `feeTypeId` 分页查询小区或 `bill_item` 明细的 endpoint。

因此目前**完全没有**：

```text
年/月 → fee_type → community → bill_item（含房产、用量、单价、金额、账单状态）
```

最小后端扩展不需要改 Schema：

1. `GET /api/admin/statistics/fee-types?year&month?`：按 `fee_type` 聚合；
2. `GET /api/admin/statistics/fee-types/{feeTypeId}/communities?year&month?`：按小区聚合；
3. `GET /api/admin/statistics/fee-types/{feeTypeId}/items?year&month?&communityId&page&pageSize`：`bill_item` 分页明细，JOIN `bill → fee_standard → fee_type → property_unit → building_unit → building → community`，返回完整位置、数量、单价、金额、账单状态和必要读数；
4. 对应 Query/VO/Mapper XML/Service/Controller，不改现有时间维度端点。

## 九、重复请求审计

### 9.1 日志证据

已有访问日志记录了同参数、数十毫秒内的重复请求，例如：

- residents：`23:49:24.392` / `.442`；
- rentals：`23:49:28.572/.574` / `.631`；
- bills：`23:49:46.876` / `.924`；
- fee standards：`23:49:48.749` / `.775`；
- payments：`23:49:54.429` / `.477`；
- overdue overview + list：`23:49:59.459/.488` 后又 `.537/.561`；
- properties：`23:50:31.051` / `.080`。

因此“重复请求存在”是确认事实。

### 9.2 源码责任边界

| 页面 | 单个 View 中的静态调用点 | 静态判断 |
| --- | --- | --- |
| Residents | 只有 `onMounted(...; load())` | 找不到第二个显式 `load()`。 |
| Rentals | 只有 `onMounted(...; load())` | 找不到第二个显式 `load()`。 |
| Properties | 只有 `onMounted(...; load())` | 找不到第二个显式 `load()`。 |
| Bills | 只有 `onMounted(...; load())` | 找不到第二个显式 `load()`。 |
| Fee tabs | `@tab-change="onTab"`，每次切 Tab 调一次相应 loader | 逻辑本身单次；日志显示的双调用不是这一个函数能在初始状态自行解释。 |
| Monthly statistics | `watch(() => route.fullPath, load)` **加** `onMounted(load)` | 已确认会在首次挂载产生两次 `load()`。 |

对 residents/rentals/properties/bills/fees，静态源码排除了“onMounted + immediate watch 同时存在”的常见原因。结合日志，下一轮必须优先确认是否存在：同页双实例、开发时 HMR/页面自动重载、双前端入口、重复导航或应用级重挂载。`frontend/index.html` 只有一个 `#app` 和一个 `/src/main.js`，故也没有发现单 HTML 双入口。结论应是“**已证实重复，根因尚未闭环**”，不能据此乱删页面加载逻辑。

## 十、ResizeObserver 审计

日志明确记录两次浏览器异常：

- `/fees`：`ResizeObserver loop completed with undelivered notifications.`；
- `/properties`：同一异常。

客户端监控会把所有 `window.error` 上报为 ERROR；它没有屏蔽该异常。

源码中唯一直接使用 `ResizeObserver` 的位置是 `frontend/src/components/EChart.vue`：观察图表根节点后，在回调中直接同步调用 `chart.resize()`。这属于已知可能触发循环的模式，适合优先排查。

但 `/fees` 与 `/properties` 当前没有引用 `EChart.vue`，且两页都有 Element Plus `el-table`、Drawer、动态加载和布局变化。因此当前只能得出：

- EChart 的 observer 是候选来源；
- Element Plus table/drawer/tab 的内部 observer 也是候选来源；
- 不能在没有堆栈或最小复现的情况下过滤/降级该错误。

下一轮排查方式：在开发模式下给 EChart observer、Drawer 打开、Tab 切换、表格数据替换分别加**仅开发期且无敏感数据**的来源标记，逐项复现；若确认是 EChart，再用 `requestAnimationFrame` 合并 `resize()` 并在销毁后取消，而不是吞错误。

## 十一、年度“12 个月趋势”审计

`AnnualStatisticsView.vue` 标题固定为“12 个月收费趋势”，但图表横轴直接 `monthly.value.map(...)`。后端 `selectMonthlyCollection` 从 `v_monthly_collection_summary WHERE billing_year = year` 返回实际有账单的月份；当前数据只有 1–7 月，因此前端只画 7 个点。

两种可选方案：

| 方案 | 做法 | 影响 |
| --- | --- | --- |
| A：固定 12 月 | 前端（或统计 service）补齐 1–12 月，缺失月份金额/账单数为 0；可加“尚未出账”提示 | 视觉完整，保持“12 个月”标题；需避免用户误读为已生成零金额账单。 |
| B：改标题 | 改为“2026 年已产生账单月份收费趋势” | 最小变更、绝不引入补零语义，但视觉上只呈现已发生月份。 |

## 十二、问题优先级

### P0：下一轮应优先修复

1. 统一所有大表的可见分页控件：总数、pageSize、页码、上一页/下一页、跳页，并将其置于固定工作区可见位置。
2. 对 `/properties` 非空关键词做运行期端到端验证，定位“用户体验中无法搜索”是否为渲染/交互问题；静态链目前完整。
3. 账单 UI 暴露已存在的 `propertyKeyword` 搜索，并补齐账单/收费子页分页缺失项。
4. 修复已确认的 `MonthlyStatisticsView` 首次 `watch + onMounted` 双加载；同时用运行期证据定位其它页面的双请求，不可盲改。

### P1：使页面成为可用后台工作台

1. 租赁拆分“挂牌列表 / 数据分析”并为表格设置内部滚动高度。
2. 账单列表和统计账单明细返回/显示完整位置；账单号降为次要信息。
3. 完整展示已有账单详情：位置、账期、实收、状态、截止日、项目读数、支付方式和状态。
4. 实现费用类别 → 小区 → `bill_item` 明细第二维下钻。
5. 对 `/fees`、`/properties` 的 ResizeObserver 先定位后修复。

### P2：一致性与呈现优化

1. 增加户型枚举中文映射，数据库仍保持英文枚举。
2. 将租赁页 KPI 统一为“本期已出租挂牌”，明确 SELF/AGENCY 的统计口径。
3. 选择固定补齐 12 个月或改年度趋势标题。
4. 统一枚举展示、账单号 UI 权重和详情字段格式。

## 十三、建议的下一轮实施边界

本报告建议下一轮只做“交互稳定化”：`PaginationBar + usePagedQuery`、表格工作台高度、已确认的双调用修复、非空搜索验证、账单呈现补全、中文枚举映射、年度趋势口径。费用类别第二维下钻可作为同一稳定化轮的后半段或单独小阶段，不应与新的居民/中介/地图业务混做。

