# Stage UI-R4：户型图面积 Variant 与完整预览修复报告

## A. 图片审计

- 原候选：17 张；仅审计 `backend/src/main/java/com/smartproperty/platform/pic`。
- 完全重复：1 组，即 `42d7370c-cf5b-4085-8eba-85f430c73870.webp` 与带 `(1)` 的同名副本；二进制 SHA-256 一致，未重复导入。
- 最终选用：12 张；未使用：5 张（含上述重复副本）。
- 原始候选没有删除、移动或重命名；选中图片已复制到 `pic/normalized/`，并复制到既有 classpath 导入资源目录。

| 原候选文件 | 规范资源文件 | 户型编码 | 建筑面积（㎡） | FileObject ID | 类型 |
| --- | --- | --- | ---: | ---: | --- |
| `647020c8-06c3-44e1-a488-09f100fde9df.webp` | `A1_56_50.webp` | `LT-1B1L-58` | 56.50 | 6 | WEBP |
| `屏幕截图 2026-08-30 165347.png` | `A1_58_00.png` | `LT-1B1L-58` | 58.00 | 2 | PNG（复用 V9） |
| `c1944650-92e4-48fe-b056-265150a58460.webp` | `A1_59_50.webp` | `LT-1B1L-58` | 59.50 | 7 | WEBP |
| `1311061461160_000.webp` | `B0_66_50.webp` | `LT-2B-68` | 66.50 | 8 | WEBP |
| `024455m50msm9j0sm0bghr.png` | `B0_68_00.png` | `LT-2B-68` | 68.00 | 3 | PNG（复用 V9） |
| `1400x989.jpg` | `B0_69_50.jpg` | `LT-2B-68` | 69.50 | 9 | JPEG |
| `20d58f97-0fde-451f-bfc4-a4a76108ccdc.webp` | `B1_87_50.webp` | `LT-2B1L-89` | 87.50 | 10 | WEBP |
| `屏幕截图 2026-08-30 165311.png` | `B1_89_00.png` | `LT-2B1L-89` | 89.00 | 4 | PNG（复用 V9） |
| `42d7370c-cf5b-4085-8eba-85f430c73870.webp` | `B1_90_50.webp` | `LT-2B1L-89` | 90.50 | 11 | WEBP |
| `1400x989 (1).jpg` | `B2_106_50.jpg` | `LT-2B2L-108` | 106.50 | 12 | JPEG |
| `屏幕截图 2026-08-30 165330.png` | `B2_108_00.png` | `LT-2B2L-108` | 108.00 | 5 | PNG（复用 V9） |
| `fce26fc2-97ac-44a5-a577-449a55cc9f62.webp` | `B2_109_50.webp` | `LT-2B2L-108` | 109.50 | 13 | WEBP |

## B. 数据模型与解析优先级

当前 Flyway 基线为 V9，因此新增 V10 `layout_template_floor_plan_variant`；没有修改任何 V1–V9 文件。

V10 以 `UNIQUE(layout_template_id, gross_area)` 保存精确 `DECIMAL(10,2)` 面积关系，并通过 `file_object_id` 引用已有对象存储元数据。没有改动 528 套房产的 `gross_area`，也没有把图片复制到每一套房产。

解析优先级：

1. `property_floor_plan` 的房产专属图；
2. `layout_template_floor_plan_variant` 的“户型 + 精确建筑面积”图；
3. V9 `layout_template_floor_plan` 默认模板图；
4. 无图状态。

中央面积（58.00、68.00、89.00、108.00）直接复用了已存在的 V9 `FileObject`，没有重复上传对象。

## C. MinIO 与导入链

`TemplateFloorPlanController` → `FloorPlanVariantImportService` → `FloorPlanVariantImportServiceImpl` → Mapper → `FileObject`/既有 MinIO Client。

新对象写入既有 `property-assets` bucket 的 `floorplans/layout-variants/` 前缀；原有 bucket、凭据和启动逻辑均未改动。导入器先针对当前图片 SHA-256 查找已存在的活动 `FileObject`，可复用时不再上传。

## D. 前端预览修复

`PropertyDrawer.vue` 现在：

- 通过 `el-image` 使用 `fit="contain"` 完整展示平面图，不裁切上下左右；
- 缩略图点击后直接使用 Element Plus 原生遮罩大图预览；
- 显示“房产专属户型图 / 标准户型参考图 · 面积 / 户型模板参考图”语义；
- 空状态明确说明三层回退规则；
- 维持现有深绿、白色、浅灰 UI 体系。

## E. 验收结果

| 验收项 | 结果 |
| --- | --- |
| Flyway V10 执行 | PASS |
| 12 个真实 layout + gross_area Variant | PASS |
| 后端 `mvn -DskipTests compile` | PASS |
| 前端 `npm run build` | PASS |
| UI 静态质量检查 | PASS |
| A1 56.50 详情图下载 | PASS，HTTP 200，`image/webp` |
| B0 66.50 详情图下载 | PASS，HTTP 200，`image/webp` |
| B1 87.50 详情图下载 | PASS，HTTP 200，`image/webp` |
| B2 106.50 详情图下载 | PASS，HTTP 200，`image/jpeg` |
| B0 66.50 ≠ 68.00 ≠ 69.50 | PASS，解析 FileObject 分别为 8、3、9 |
| 两套 B0 68.00 房产复用 | PASS，房产 3 与 6 均解析至 FileObject 3 |

用户最终的浏览器人工验收仍建议检查：缩略图完整显示、点击大图预览、不同 B0 面积显示不同图、同面积 B0 显示相同图。未使用自动化代填任何登录凭据，也未伪造浏览器截图。

## F. 本轮边界声明

没有修改高德地图、停车证、通知、账单、人员、JWT、Spring Security、DataScope、Redis、MinIO 核心配置、全局 Jackson、全局分页、既有 `property_unit` 数据、`layout_template` 定义、V1–V9 历史 Migration 或既有 V9 默认图。
