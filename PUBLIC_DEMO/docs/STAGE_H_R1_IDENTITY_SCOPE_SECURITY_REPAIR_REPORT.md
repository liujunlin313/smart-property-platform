# Stage H-R1：身份分流、遗留 API 权限与物业 DataScope 收口报告

## 本轮范围

本轮只修复 Stage H-R0 已确认的三个问题：

1. `PERSON_USER` 登录后的默认路由与非法 `redirect` 回跳；
2. 居民可直接读取旧 `/api/v1` 资产接口；
3. `PROPERTY_STAFF` 在旧资产、Dashboard、统计中缺少统一 Community DataScope 的情况。

未新增数据库表、字段或迁移；未改动 Redis、MinIO、高德地图、Jackson 配置和既有 V1--V7 数据库对象。

## 实施结果

### 1. 前端身份分流

- `frontend/src/router/index.js`
  - `PERSON_USER` 的默认主页为 `/resident`；
  - `/assets`、Dashboard、统计路由均限制为 `SYSTEM_ADMIN` 或 `PROPERTY_STAFF`；
  - 路由守卫对登录后目标地址作角色校验，不合规的 `redirect` 回到该账号的默认主页。
- `frontend/src/views/LoginView.vue`
  - 只接受站内绝对路径作为候选 `redirect`，并在 `router.replace` 前调用统一角色校验。
- `frontend/src/layouts/AppLayout.vue`
  - 用户身份文字按真实角色显示；
  - Dashboard、月度统计、年度统计对 `PROPERTY_STAFF` 显示入口。

### 2. 旧资产 API 权限封口

- `SecurityConfig` 对旧资产只读 URL 显式要求 `SYSTEM_ADMIN` 或 `PROPERTY_STAFF`：
  - `/api/v1/dashboard/summary`
  - `/api/v1/communities/**`
  - `/api/v1/buildings/**`
  - `/api/v1/building-units/**`
  - `/api/v1/properties/**`
  - `/api/v1/layout-templates`
- 其他 `/api/v1/**` 保持仅 `SYSTEM_ADMIN`，没有删除任何既有接口。
- 居民请求会在 Spring Security 层被拒绝，不会进入 Controller 或 Service。

### 3. 统一物业 DataScope

- 旧资产层的 Community、Building、BuildingUnit、PropertyUnit、AssetDashboard 服务改为复用现有 `AuthorizationService + DataScopeService`。
- Mapper XML 将 scope 下沉至 `building.community_id`；管理员使用全局 Scope，物业人员只查询授权小区。
- 居民汇总、Dashboard、月度/年度统计同样使用当前身份的 Scope。
- 月度收费统计不再读取全局聚合视图 `v_monthly_collection_summary` 后伪过滤，而是从 `v_bill_financial_summary` 关联房产层级后再聚合。因此账单数量、应收、实收、欠费和收缴率均为真实范围内结果。
- 月度下钻在查询前继续校验指定小区属于当前 Scope。

## 验证结果

| 检查 | 结果 |
| --- | --- |
| 后端编译（`mvn -DskipTests compile`） | PASS |
| 后端测试（`mvn test`） | PASS |
| 前端生产构建（`npm run build`） | PASS |
| `admin`、`lzx`、`staff_scope_a`、`staff_scope_c` 登录 | PASS（均为 HTTP 200） |
| `lzx` 直连 `GET /api/v1/properties` | PASS（正确拒绝为 HTTP 403） |
| `staff_scope_a` 旧社区接口 | PASS（仅 1 个：上地西里） |
| `staff_scope_c` 旧社区接口 | PASS（仅 2 个：上地西里、上地佳园） |
| 两个物业账号旧房产分页 | PASS（总数分别为 72、168，符合各自 Scope） |
| 两个物业账号 Dashboard Overview | PASS（HTTP 200；房产数分别为 72、168） |
| 两个物业账号月度统计 | PASS（均返回真实 7 个有账单月份，首月应收金额不同且符合各自 Scope） |

HTTP 验收使用临时后端端口 `8081`。测试结束后仅按本次启动进程的 PID 树回收该实例，并确认 `8081` 已释放；没有停止 `8080`、`5173` 或任何用户进程。

## 已知非阻塞项

- Maven 测试过程会出现 Mockito 动态加载 agent 的 JDK 未来兼容性警告；测试本身已通过，本轮没有修改测试基础设施。
- Vite 构建仍提示部分 ECharts/依赖打包块较大；不影响构建和功能，且不属于本轮身份与范围修复。

## Deferred

- 更细的业务域与对象级 RBAC（如住户、经纪、物业岗位的细分操作权限）仍由后续阶段完成。
- 物业工作人员的人工浏览器验收由用户在自己的前端开发服务器中完成；本轮未进行浏览器自动化测试。
