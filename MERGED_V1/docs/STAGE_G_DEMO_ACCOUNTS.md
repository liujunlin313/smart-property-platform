# Stage G 本地演示账号

所有 Stage G 演示账号均通过既有统一登录接口登录：`POST /api/v1/auth/login`。初始密码为 `123456`；数据库仅保存 BCrypt 哈希，本文不记录任何哈希、JWT、Refresh Token 或个人敏感信息。

## 物业工作人员

| Username | Role | Community Scope |
| --- | --- | --- |
| `staff_scope_a` | `PROPERTY_STAFF` | 上地西里 |
| `staff_scope_b` | `PROPERTY_STAFF` | 上地东里 |
| `staff_scope_c` | `PROPERTY_STAFF` | 上地西里、上地佳园 |
| `staff_scope_d` | `PROPERTY_STAFF` | 北京大学蔚秀园 |

## 居民代表样本

53 个居民账号均有真实 `app_account.person_id → Person → current Residence` 绑定。为避免文档成为居民信息清单，只列出人工验收最有价值的非敏感样本：

| Username | Person ID | 当前关系 / 用途 |
| --- | ---: | --- |
| `zzx` | 1 | 当前产权人、同房产租户对 |
| `zyt` | 2 | 当前产权人、同房产租户对 |
| `lzx` | 21 | Owner-occupied responsible resident |
| `lhr` | 23 | Owner-occupied responsible resident |
| `dzx` | 281 | Member resident；同时用于同房产关系验证 |
| `dyt` | 282 | Member resident / 当前租户样本 |
| `cbw` | 676 | Tenant-occupied responsible resident |

完整账号集合由受保护 Provisioning 接口按数据库现状幂等生成；如果本机需要核对，可由系统管理员再次调用该接口查看不包含密码的创建/既有统计，而不是手写数据库插入。
