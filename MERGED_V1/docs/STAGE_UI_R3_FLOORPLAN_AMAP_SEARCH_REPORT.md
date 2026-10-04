# Stage UI-R3：户型图模板与高德双向检索报告

## 1. Before（改造前确认）

- 数据库只有 `property_floor_plan`：它描述的是某一套具体房产的专属户型图，并且 `file_object_id` 具有唯一约束；原上传替换流程会回收旧对象。因此它不能安全地兼作“多个房产共用一张模板图”的关系表。
- `file_object` 与 MinIO 中没有可用于本轮四个标准户型的已有关系；`layout_template` 已存在四个对应户型编码。
- 地图已有场景、小区/楼栋 Marker、边界、坐标初始化和高德 Web Service 能力，但没有“从小区查真实 POI”或“从 POI 反查受管小区”的业务 API。

## 2. 架构决定

新增 V9：`layout_template_floor_plan`，只保存“户型模板 → FileObject”的关系；不改写 `property_floor_plan`。

展示优先级固定为：

1. 房产专属户型图（旧能力，最高优先级）；
2. 户型模板标准图（本轮回退）；
3. 无图状态。

这避免了模板图被某套房产的替换/删除流程误回收，也允许多个 `PropertyUnit` 共享同一条 `FileObject` 关系。

## 3. 标准户型图映射与导入结果

| 用户标签 | `layout_template` 编码 | 模板 ID | 项目资源文件 | FileObject ID | 导入结果 |
| --- | --- | ---: | --- | ---: | --- |
| A1 | `LT-1B1L-58` | 3 | `bootstrap/floorplans/layout_A1_58m2.png` | 2 | IMPORTED |
| B0 | `LT-2B-68` | 4 | `bootstrap/floorplans/layout_B0_68m2.png` | 3 | IMPORTED |
| B1 | `LT-2B1L-89` | 5 | `bootstrap/floorplans/layout_B1_89m2.png` | 4 | IMPORTED |
| B2 | `LT-2B2L-108` | 6 | `bootstrap/floorplans/layout_B2_108m2.png` | 5 | IMPORTED |

原始用户图片保留在 `backend/src/main/java/com/smartproperty/platform/pic`，导入使用其副本；没有移动、删除或覆盖原图。

## 4. 实现链路

### 4.1 户型图

`PropertyDrawer.vue` → `GET /api/v1/properties/{id}/floor-plan` → `FloorPlanService`（专属图优先、模板图回退）→ `FileObject` → 已有 MinIO 下载链路。

管理员导入入口：`POST /api/admin/floor-plan-templates/import-standard`。导入操作按模板关系幂等：已有关联时返回 `EXISTS`，不会再次生成数据库关系或改动专属户型图。

### 4.2 地图双向检索

小区 → POI：

`MapView.vue` → `GET /api/admin/map/search/nearby` → `MapPoiSearchService` → DataScope 小区校验 + 已有 `GeoLocationService.searchNearbyPoi` → 高德真实 POI → 地图临时 POI Marker。

POI → 受管小区：

`MapView.vue` → `GET /api/admin/map/search/poi-candidates` → 选择 POI → `POST /api/admin/map/search/reverse-communities` → 已有 `GeoLocationService.searchPoi` → 受 DataScope 限制的小区查询 → 服务端 Haversine 直线距离排序。

前端不会提交小区坐标，也不会在浏览器侧计算距离；原地图 Scene、Marker、坐标和边界逻辑保持不变。

## 5. 验收结果

| 项目 | 结果 | 证据 |
| --- | --- | --- |
| V9 Flyway | PASS | 空库升级后版本为 V9；原 V1–V8 未修改 |
| 四张模板图导入 | PASS | A1/B0/B1/B2 均已建立 FileObject + 模板关系 |
| A1/B0/B1/B2 实际下载 | PASS | 四个 `/floor-plan` 响应均为 HTTP 200、`image/png` |
| B0 共享关系 | PASS | 两个不同 `PropertyUnit`（520、521）均回退到同一 FileObject ID 3 |
| 小区 → 真实周边 POI | PASS | 对受管小区的真实高德检索返回 25 条 POI，首条含坐标 |
| POI → 受管小区 | PASS | 真实 POI 反查返回 4 个受 DataScope 限制的小区及服务器计算距离 |
| 后端编译 | PASS | `mvn -DskipTests compile` |
| Vue 生产构建 | PASS | `npm run build` |
| UI 静态质量检查 | PASS | Impeccable detector 无问题 |

### 浏览器视觉验收

临时 8081/5174 已用于 API 验收。登录后的页面视觉验收没有由自动化代填管理员密码，因此本报告不伪造截图或“已人工点击”的结论。

人工验收步骤：以 `SYSTEM_ADMIN` 登录后，打开 `/map`：

1. 在“从管理小区查周边设施”选择小区、输入关键词并搜索，确认 POI 列表与金色临时 Marker；
2. 在“从 POI 反查管理小区”搜索并选择 POI，确认只显示有数据权限的小区与距离；
3. 打开任一 A1/B0/B1/B2 房产详情，确认“户型图”提示为模板回退；对 B0 打开两套不同房产，确认均显示相同标准图。

代码复核已确认：`PropertyUnitMapper.xml` 的列表与详情查询均以 `COALESCE(专属 FileObject, 模板 FileObject)` 返回 `floorPlanFileId`；`PropertyDrawer.vue` 因而会请求同一条既有下载接口并得到模板回退图。只有专属图和模板图都不存在时，抽屉才显示无图状态。

## 6. 安全与边界声明

- 没有修改 V1–V8、JWT、Spring Security、Redis、MinIO 全局配置、DataScope 规则、已有地图坐标或地图场景接口。
- 没有删除或重置 MySQL、MinIO bucket、已有对象或原先 `property_floor_plan` 能力。
- 所有 POI/反查接口保留既有管理端权限与 DataScope；未在日志或响应中暴露高德、数据库、MinIO、JWT 等密钥。
- 临时测试仅使用 8081/5174；未占用或停止用户的 8080/5173，也未停止已有 Redis/MinIO。

## 7. Deferred（明确未扩大范围）

- 不做 Redis 缓存、热门 POI 预取、物业专用 POI 收藏、路线规划或距离阈值业务判定。
- 不把模板图自动复制成各房产的专属图；管理员后续上传专属图仍可自然覆盖模板展示。
- 不对现有户型图作 OCR、人工修图或坐标偏移。
- 不用浏览器自动化代替用户最终人工业务验收。
