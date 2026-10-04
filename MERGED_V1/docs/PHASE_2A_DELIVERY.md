# Phase 2A 交付说明

## 交付结果

Phase 2A 已形成真实可运行链路：

```text
Vue 3 管理工作台
        ↓ 同源 /api/v1
Spring Security + JWT + MyBatis
        ├── MySQL 8 / Flyway V1～V3
        └── MinIO 私有对象存储
```

前端、后端、数据库和对象存储并非分别“假运行”。`verify-phase2a.ps1` 会在 Windows PowerShell 5.1 中启动隔离环境并依次验证：

1. 前端生产构建和 Spring Boot 可执行 JAR。
2. Flyway V1～V3 迁移。
3. 匿名请求返回 401。
4. 管理员和只读游客 JWT 登录。
5. 小区、楼栋、单元、房产完整写链。
6. 面积未填强制 `DRAFT`。
7. 游客读成功、写返回 403。
8. 旧 `version` 写入返回 409。
9. PNG 文件头校验、MinIO 上传、数据库元数据和鉴权下载。
10. Refresh Token 轮换、退出登录、审计与文件记录断言。
11. 产权份额不是 1 时捕获 `ERROR 3819`，同一房产出现第二个当前产权人时捕获 `ERROR 1062`。

实际结果见 `database/evidence-phase2a/VERIFICATION_SUMMARY.txt`。

## 安全边界

- 后端默认只监听 `127.0.0.1`，不会直接暴露到局域网。
- 账号密码只保存 BCrypt 散列；启动账号来自环境变量，不写死在 Java 注释或仓库。
- Access Token 为 10 分钟 JWT，包含唯一 `jti`，只保存在浏览器内存。
- Refresh Token 为随机值，只通过 `HttpOnly + SameSite=Strict` Cookie 传递；数据库只保存 SHA-256 散列并在刷新时轮换。
- 连续失败 5 次锁定账号 15 分钟；管理员和中介机构未来只由系统管理员建立。
- 页面隐藏写按钮只是体验层，所有写权限由后端再次校验。
- 文件桶保持私有；下载必须经过应用鉴权；上传限制 10MB 并校验真实图片魔数。
- 业务资产不硬删除，使用 `DRAFT/ACTIVE/INACTIVE/ARCHIVED` 生命周期和审计记录。

## V3 关系变化

- 新增账号、全局角色、中介作用域角色和刷新会话。
- 小区、楼栋、单元、房产和户型模板加入 `version` 乐观锁。
- 楼栋加入 GCJ-02 经纬度与位置来源，为 Phase 2C 高德地图预留。
- 房产加入 `DRAFT` 和 `occupancy_status`：`UNKNOWN/VACANT/OWNER_OCCUPIED/AVAILABLE_FOR_RENT/TENANT_OCCUPIED`。
- `gross_area` 允许在草稿阶段为 `NULL`，非草稿必须大于 0。
- `ownership_share` 固定为 1，并通过生成列唯一约束保证每套房最多一个当前产权人；产权转移仍可保留历史区间。
- 新增 `file_object`、`property_floor_plan` 和 `asset_change_audit`。

## 本轮明确不做

- Phase 2B 的个人用户、中介管理员、申请/同意、合同 PDF/DOCX、消息抄送和出租状态改变。
- Phase 2C 的真实高德地图、POI 核验、附近搜索和空间索引。
- 物业收费、车辆车位、家具二维编辑器的页面和接口。

这些模块已经有数据库骨架或设计结论，但不会为了页面“看起来完整”而用假接口混进 Phase 2A。

## 已发现并修正的真实问题

- Windows PowerShell 5.1 对 UTF-8 无 BOM 脚本解析异常。
- 带空格的 MySQL 安装路径在 `Start-Process` 参数中需要显式引用。
- Phase 1 的多人共有演示数据与新“单一产权人”结论冲突，已统一修改并重新回归 Phase 1。
- MyBatis 对 Java `record` 的位置映射会把 `DATETIME` 误装入整数列，已改为按参数名映射。
- 同一秒签发的相同 JWT 可能字节一致，已加入唯一 `jti`。
- 当前磁盘低于新版 MinIO 的安全空闲阈值，日常运行需要先释放空间。
