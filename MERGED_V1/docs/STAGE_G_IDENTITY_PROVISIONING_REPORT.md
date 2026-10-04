# Stage G 身份与账号 Provisioning 报告

## 结果摘要

- `app_account`：59 个账号。
- 本轮新增并实际验证：4 个 `PROPERTY_STAFF` 账号、53 个绑定真实 `Person` 的 `PERSON_USER` 居民账号。
- 沿用既有 `PERSON_USER` 角色；没有另建第二套登录、注册或认证体系。
- 所有账号仍通过唯一入口 `POST /api/v1/auth/login` 登录；密码使用既有 `PasswordEncoder` 的 BCrypt 哈希保存。Stage G 演示账号初始密码统一为 `123456`，不在运行日志输出。
- 没有删除、重置或重建任何业务数据；没有修改 V1～V5。

## 物业工作人员与社区作用域

| Username | Role | 数据库作用域 |
| --- | --- | --- |
| `staff_scope_a` | `PROPERTY_STAFF` | 社区 4（上地西里） |
| `staff_scope_b` | `PROPERTY_STAFF` | 社区 5（上地东里） |
| `staff_scope_c` | `PROPERTY_STAFF` | 社区 4（上地西里）+ 社区 6（上地佳园） |
| `staff_scope_d` | `PROPERTY_STAFF` | 社区 7（北京大学蔚秀园） |
| `admin` | `SYSTEM_ADMIN` | 全局，不经过 `account_community_scope` 限制 |

`staff_scope_a` 与 `staff_scope_c` 共同覆盖社区 4，且 `staff_scope_c` 同时覆盖两个社区，已形成真实 N:M Scope 关系，而不是按用户名硬编码分支。

## 居民账号与业务关系

居民账号由当前有效 `Residence (end_date IS NULL)` 筛选产生，并绑定 `app_account.person_id`。用户名使用姓名首字母；冲突时使用稳定后缀，例如 `lhr`、`lhr002`。重跑不会为已有 Person 再创建账号。

最终样本分布：

| 口径 | 数量 |
| --- | ---: |
| `PERSON_USER` 账号 | 53 |
| 当前 `OWNER_OCCUPIED + RESPONSIBLE` 居民 | 50 |
| 当前 `TENANT_OCCUPIED + RESPONSIBLE` 租户 | 1 |
| 当前 `MEMBER` 居民 | 2 |
| 有当前 Ownership 记录的账号 | 52 |
| 同一房产同时存在已登录产权人与租户的样本对 | 3 |

已确认的同房产样本（只记录系统主键和用户名，不写姓名、手机、证件号）：

| 房产 | 产权人账号 / Person | 当前租户账号 / Person |
| --- | --- | --- |
| `102` | `zyt` / 2 | `dyt` / 282 |
| `102` | `zzx` / 1 | `dzx` / 281 |
| `202` | `dzx` / 281 | `cbw` / 676 |

## 实现边界

Provisioning 入口为仅 `SYSTEM_ADMIN` 可调用的：

`POST /api/admin/accounts/provision-stage-g`

链路保持为 `Controller → DemoAccountProvisionService → DemoAccountProvisionServiceImpl → DemoAccountProvisionMapper → Mapper XML → MySQL`。它先读取现有账号、角色、Scope 与候选有效居住关系，再计算并写入缺失项；不会执行删除后重建。

为保证未来居民端可从服务端确认身份，JWT 现包含非空 `personId` 声明。`SpringSecurityActorContextProvider` 已将其读取为 `ActorContext.personId`；前端不需要、也不能提交 personId 或角色来声明身份。

## 最小运行验证

临时后端绑定 `127.0.0.1:8081` 时已验证：

- `SYSTEM_ADMIN` 登录成功；
- `staff_scope_a` 登录成功，并只取得 1 个社区根节点；
- `staff_scope_c` 登录成功，并取得 2 个社区根节点；
- 一个真实产权人和一个真实租户均可登录；两者 JWT 都包含 `PERSON_USER` 和非空 `personId`；
- 再次调用 Provisioning 未新增重复账号。

运行验证完成后，临时进程会被关闭；用户的 8080、5173、MySQL、Redis 与 MinIO 不在本报告操作范围内。
