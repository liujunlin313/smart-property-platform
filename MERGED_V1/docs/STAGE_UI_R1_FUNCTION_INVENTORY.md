# Stage UI-R1 Before 功能清单

本清单在 UI-R1 修改前根据现有 Vue Router、Layout、视图组件和 API 模块建立。UI-R1 允许重排、分组、改善表达，但不允许删除下列有效入口、读写操作或权限边界。

## 管理端（AppLayout）

| 路由 | 原页面能力 | 可访问角色 | 关键 API / 操作 |
| --- | --- | --- | --- |
| `/assets` | 资产层级树、房产筛选、房产 Drawer、管理员资产录入/编辑 | SYSTEM_ADMIN、PROPERTY_STAFF | 旧 `/api/v1` 资产链，后端 Scope |
| `/properties` | 跨小区房产筛选、分页、详情 | SYSTEM_ADMIN、PROPERTY_STAFF | `/api/admin/properties` |
| `/map` | 高德底图、范围/楼栋 Marker、搜索、边界校准 | SYSTEM_ADMIN、PROPERTY_STAFF | `/api/admin/map/*` |
| `/residents` | 人员/居住关系检索、筛选、分页、详情 | SYSTEM_ADMIN、PROPERTY_STAFF | `/api/admin/residents*` |
| `/residents/manage` | 新建人员、查询可入住房产、办理入住 | SYSTEM_ADMIN、PROPERTY_STAFF | `/api/admin/residence-management/*` |
| `/incidents` | 异常列表、状态流转 | SYSTEM_ADMIN、PROPERTY_STAFF | `/api/admin/incidents*` |
| `/bills` | 账单筛选、分页、账单/支付详情 | SYSTEM_ADMIN、PROPERTY_STAFF | `/api/admin/bills*` |
| `/fees` | 账单、收费标准、缴费记录、欠费概览和欠费分页 | SYSTEM_ADMIN、PROPERTY_STAFF | `/api/admin/fees/*` |
| `/notifications` | 目标选择、TO/CC、发送、发送历史和详情 | SYSTEM_ADMIN、PROPERTY_STAFF | `/api/admin/notifications*` |
| `/rentals` | 挂牌摘要、筛选、图表、列表和详情 | SYSTEM_ADMIN | `/api/admin/rentals*` |
| `/dashboard` | 房产/居住/收费/挂牌概览与图表、逾期 Top | SYSTEM_ADMIN、PROPERTY_STAFF | `/api/admin/dashboard/*`，后端 Scope |
| `/statistics/monthly` | 月度趋势、月→小区→账单下钻 | SYSTEM_ADMIN、PROPERTY_STAFF | `/api/admin/statistics/*`，后端 Scope |
| `/statistics/annual` | 年度 12 月趋势、费用构成、进入月度下钻 | SYSTEM_ADMIN、PROPERTY_STAFF | `/api/admin/statistics/*`，后端 Scope |
| `/settings` | 现有系统设置占位入口 | 当前 AppLayout 可见 | 无新增业务逻辑 |

## 居民端（ResidentLayout）

| 路由 | 原页面能力 | 可访问角色 | 关键 API / 操作 |
| --- | --- | --- | --- |
| `/resident` | 居民摘要 | PERSON_USER | `/api/resident/me/summary` |
| `/resident/residences` | 本人当前居所 | PERSON_USER | `/api/resident/me/residences` |
| `/resident/properties` | 本人产权房产 | PERSON_USER | `/api/resident/me/properties` |
| `/resident/bills` | 本人账单 | PERSON_USER | `/api/resident/me/bills` |
| `/resident/notifications` | 本人通知 | PERSON_USER | `/api/resident/me/notifications` |
| `/resident/incidents` | 本人异常记录与新建异常上报 | PERSON_USER | `/api/resident/me/incidents` |

## 不可改变的权限与数据边界

- `SYSTEM_ADMIN`：全局数据范围。
- `PROPERTY_STAFF`：只由后端 `DataScope` 返回授权小区的数据；UI 不做全量数据读取或前端过滤。
- `PERSON_USER`：只由 JWT 绑定的 `personId` 查询本人数据；不得增加可由浏览器指定的 `personId` 参数。
- 居民不可访问 `/assets` 或管理端旧 `/api/v1` 资产读接口。

## UI-R1 After 核对

- 管理端 14 个路由与居民端 6 个路由均保留；未删除任何 router record、API 模块或角色路由守卫。
- 管理端导航改为“工作台、资产与空间、居民服务、费用管理、物业运营、数据分析”六组；管理员才可见“租赁挂牌”。
- 管理端继续将 `PROPERTY_STAFF` 的小区范围展示为说明文字，实际范围仍只由后端 `DataScope` 决定。
- 居民门户保留原有六项查询/上报 API；异常上报仍调用原 `residentApi.reportIncident`，没有增加浏览器可控 personId。
- 房产、账单、入住办理、居民异常、居民门户和数据总览均已接入统一标题、卡片、状态标签或空态组件；未改变相应请求方法与请求参数契约。
