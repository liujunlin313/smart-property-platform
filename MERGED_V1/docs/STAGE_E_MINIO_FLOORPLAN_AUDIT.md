# Stage E · 户型图与 MinIO 链路只读审计

审计范围：当前 `_MERGED_V1` 源码与既有迁移脚本。本文不修改数据库、MinIO 对象、接口实现或页面行为。

## 结论

当前项目已经具备“具体 `PropertyUnit` 的整套户型图”上传、持久化、读取和旧版资产工作台预览能力；它不是模板图，也没有 `Room` 拆分。上传对象存入 MinIO，MySQL 只保存对象元数据和当前关联关系。

但 Stage 1 管理端的 `/properties` 详情抽屉尚未接入这条能力。因此当前能力在旧 `/assets` 工作台可用，在新的 `/properties` 管理端不可见。这是 UI/API 链路尚未接通，不是 MinIO 存储失败。

## 已实现链路

```text
PropertyUnit
  -> POST /api/v1/properties/{propertyUnitId}/floor-plan
  -> FloorPlanService
  -> MinIO bucket（对象二进制）
  -> file_object（对象元数据）
  -> property_floor_plan（当前户型图关联）
  -> GET /api/v1/properties/{propertyUnitId}/floor-plan
  -> assets.js fetchFloorPlanBlob
  -> PropertyDrawer.vue 预览
```

### 数据模型与基数

- `property_floor_plan.property_unit_id` 是主键：一套房产当前至多关联一张户型图。
- `property_floor_plan.file_object_id` 是唯一键：一份活动文件不会被多套房产共享。
- 所以当前关系是 `PropertyUnit 0..1 ↔ 0..1 FileObject`，语义为“该具体整套房产的当前户型图”。
- 上传新图时，旧文件元数据被标记为 `DELETED`，旧对象在数据库事务提交后尝试从 MinIO 删除；页面和当前关联不提供历史版本浏览。

### 文件安全与可用性

- 只接受实际文件头为 JPEG、PNG、WebP 的图片，拒绝 SVG；最大 10MB。
- 对象键使用 `property-units/{propertyUnitId}/floor-plan-{UUID}`，不直接使用用户上传文件名。
- MinIO 不可用时，户型图 API 返回可识别的“文件存储暂不可用”，房产的数据库读写仍可继续。
- 启动期间会检查/按需创建既有 bucket；本审计未读取、展示或变更任何本机凭据与对象数据。

## 当前页面覆盖情况

| 场景 | 当前状态 | 证据 |
| --- | --- | --- |
| 旧资产工作台 `/assets` | 已支持上传和预览 | `AssetsView.vue` + `PropertyDrawer.vue` |
| 旧资产详情抽屉 | 已支持 Blob 预览与替换上传 | `PropertyDrawer.vue` |
| Stage 1 管理端 `/properties` 列表 | 未展示户型图状态 | `PropertyView.vue` |
| Stage 1 管理端 `/properties` 详情抽屉 | 未展示图片，也未调用户型图读取接口 | `PropertyView.vue` |
| 户型模板图 | 本轮未审计为当前可用独立链路 | 不与具体房产户型图混同 |

## 发现的历史残留与边界

1. 初始 Schema 仍有 `property_unit.floor_plan_object_key` 字段；当前活动文件链路实际使用 `file_object + property_floor_plan`。当前代码没有把该旧字段作为户型图读取来源。
2. 前端存在两条共用同一数据库/MinIO 的房产 UI：旧 `/assets`（`/api/v1/**`）和 Stage 1 `/properties`（`/api/admin/**`）。它们不是两套对象存储，但后者尚未桥接户型图能力。
3. 当前 `FloorPlanService` 是单个服务类而非 `Interface + Impl` 分层；这是既有链路结构，本轮未重构。
4. 当前接口提供上传、读取与替换；没有面向用户的“删除户型图”端点，也没有版本回溯页面。

## 下一轮最小实施建议（非本轮修改）

若目标是“Stage 1 管理端房产详情显示真实户型图”，建议保持现有 MinIO 表和 `/api/v1/properties/{id}/floor-plan` 文件接口不变，按以下顺序增量接通：

1. 在管理员房产详情 VO 中只增加“是否已上传户型图”的安全状态字段，避免列表逐行下载图片。
2. 在 `/properties` 详情抽屉增加“户型图”区域；详情打开后按需调用现有 Blob 下载接口并用 object URL 预览。
3. 复用现有 `fetchFloorPlanBlob`、对象 URL 释放和文件类型限制，不新建第二套 MinIO 客户端或第二套对象表。
4. 需要在管理端上传/替换时，再补齐管理员授权和该抽屉的上传交互；删除与历史版本应作为单独业务决定，不能由本审计擅自引入。

