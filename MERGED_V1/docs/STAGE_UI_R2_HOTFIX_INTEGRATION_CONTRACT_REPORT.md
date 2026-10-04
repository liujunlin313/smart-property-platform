# Stage UI-R2 Hotfix：入住资格 SQL 与居民车证 API 契约

## 用户现象与关键证据

1. 管理员办理租赁入住时，`claimProperty` 与 `insertResidence` 已成功，随后 `insertParkingEligibility` 报 MySQL `Unknown column 'person_id' in 'field list'`，接口返回 500。
2. 居民“我的车证”页面请求单数路径 `/api/resident/me/parking-permits/eligibility`，HTTP 404。

## 审计结论

- V8 原始 Migration 与当前 `community_db` 的 `residence_parking_eligibility` 实际表一致：只有 `residence_id`、资格字段和审计字段；没有 `person_id`、`property_unit_id`。
- Flyway V8 记录为成功，且此前验证通过；不存在需要修改历史 Migration 的证据。
- 失败请求后的 Person 1027 为 `ACTIVE`，无有效 Residence；失败事务的房产占用与 Residence 写入均已回滚。
- 车证资格查询在 Service 中本来就是 `List`，无资格会返回空集合；404 发生在 Controller 前，因为前端单数 URL 与后端复数集合 Mapping 不一致。

## 最小修改

- `ResidenceManagementMapper.java`：资格写入命令仅保留 `residenceId`、`eligible`、`reasonCode`、`remark`。
- `ResidenceManagementServiceImpl.java`：租赁入住调用同步收缩，不再传递冗余人员/房产字段。
- `ResidenceManagementMapper.xml`：资格 INSERT 改为四列；人员与房产继续通过 `residence_id → residence` 关联获取。
- `frontend/src/api/stageH.js`：居民资格 API 改为 canonical 集合路径。

未修改 V1–V8 Migration，未创建 V9，未 ALTER/DROP/TRUNCATE，未改权限、DataScope、JWT、地图、Redis、MinIO 或其它业务模块。

## 最终数据与 HTTP 契约

- 资格关系：`residence_parking_eligibility.residence_id → residence.person_id / residence.property_unit_id`。
- Canonical resident URL：`GET /api/resident/me/parking-permits/eligibilities`。
- 居民端数据边界仍由 JWT `personId` 经 `ActorContextProvider` 决定；Controller 不接收浏览器传入的 personId。

## 授权 Smoke 验证

### Smoke 1：租赁入住 + 停车资格

- 复用 Person 1027；测试前为 ACTIVE 且无有效 Residence。
- 选取一套已验证可办理的空闲/可出租房产。
- 管理员登录 200；`POST /api/admin/residence-management/move-ins` 200。
- 新建 Residence 1041；对应 eligibility 记录存在，且通过 JOIN `residence` 可得到正确 Person 与 Property；房产状态同步为 `TENANT_OCCUPIED`。

### Smoke 2：居民资格集合

- 使用既有 PERSON_USER 账户登录；登录 200。
- `GET /api/resident/me/parking-permits/eligibilities` 返回 200 与 1 条数据。
- 返回房产通过数据库关系校验属于该 JWT 绑定 Person；未出现跨人员资格数据。

## 构建与清理

- 后端 `mvn -DskipTests compile`：PASS。
- 前端 `npm run build`：PASS（仅既有第三方 Rollup 注释/体积提示）。
- Smoke 使用临时 8081 后端，结束后已按 8081 占用进程 PID 与启动器 PID 精确停止；未触碰用户的 8080/5173。

## Deferred

- 未发现与本次两个契约断点直接相关的其它问题；无额外改动。
