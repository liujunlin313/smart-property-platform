# Stage UI-R2 + Parking Permit V1 交付记录

## 范围与保护边界

- 仅在现有 `MERGED_V1` 增量实现；没有重新初始化工程、数据库或账号体系。
- 历史 Flyway V1-V7、房产/产权/入住/账单/地图/通知等既有业务数据均未修改、删除或清空。
- 未新增或分配固定停车位；历史 `parking_right` 与 `parking_space` 保持原样且不参与本模块。
- 未改动 Redis、MinIO、JWT、高德、地图、既有权限范围或老页面的业务入口。

## UI-R2 收口

- `AppLayout.vue` 根据路由 `pageHeader` 避免顶栏标题和页面 `PageHeader` 重复；Dashboard、房产、账单、入住、居民异常、地图、通知与车证管理均只保留一个主标题。
- `NotificationView.vue` 的内部业务标题改为“发送与发送记录”，保留通知发送和历史两个业务区。
- `presentation.js` 提供统一 `displayLayoutType`，供资产工作台、房产管理、房产抽屉复用，前端不再显示 `TWO_BEDROOM` 等原始户型枚举。

## 车证业务实现

### 数据与迁移

- 追加 `V8__parking_permit_management.sql`，Flyway 已成功从 v7 升至 v8。
- 新表：`residence_parking_eligibility`、`parking_permit_application`、`parking_permit`。
- 复用既有 `vehicle`；新增租赁资格严格按 `residence_id` 绑定，不把资格泛化为人员永久权益。
- `parking_right` 未复用：它外键绑定固定 `parking_space`，与“车证不是车位”不兼容。
- 新增权限：`PARKING_PERMIT_VIEW`、`PARKING_PERMIT_MANAGE`，授予 `SYSTEM_ADMIN` 和 `PROPERTY_STAFF`。

### 后端链路

- 按 `Controller → Service Interface → ServiceImpl → Mapper Interface → Mapper XML → MySQL` 实现。
- 管理端：`/api/admin/parking-permits`、`/summary`、`/applications`、申请审核。
- 居民端：`/api/resident/me/parking-permits`、资格、本人车辆、本人申请与本人车证。
- 资格：产权人按当前产权自动取得资格；租户必须有当前入住记录且该记录被授予资格。
- 管理员/物业人员经 `AuthorizationService` 取得权限，并通过 `DataScopeService.requireProperty` 和查询范围限制硬性校验。
- 居民端从 `ActorContextProvider` 取当前绑定人员；不可代替他人登记车辆或提交申请。
- 价格与限制仅在服务端计算：第一张有效证 200/月、第二张 1200/月；人员总有效证最多两张，单套房产有效证最多两张；申请 1–12 个月，租赁证不超过已填写的预计结束日期。

### 前端入口

- 管理端“物业运营”新增“车证管理”：申请审核、已签发车证、真实汇总数据与分页。
- 居民端新增“我的车证”：资格、本人车辆登记、申请提交、申请/车证记录。
- 办理租赁入住新增资格录入；产权人自住不写入租赁资格表。

## 验证结果

- Backend：`mvn -DskipTests compile` PASS。
- Frontend：`npm run build` PASS。只有既有第三方 Rollup 注释和体积提示，无构建失败。
- Flyway：V8 PASS。
- 数据库只读校验：2 条租赁资格、5 辆演示车辆、5 张申请（3 通过 / 1 待审核 / 1 拒绝）、3 张车证。
- 关键不变量：重复车牌 0；任意人员或任意房产超过两张当前有效车证的记录均为 0；两项车证权限均存在。

## Deferred

- 未接入账单/缴费；本轮仅保存后端计算出的报价。
- 未做退租自动撤销、提前退租退款、续期、人工撤销或固定车位分配。
- 既有历史租赁入住未自动补写资格，避免篡改旧业务语义；可在后续按业务规则单独处理。
- 未做浏览器自动化点击测试；保留人工验收。
