# Stage G 居民端后续蓝图（本轮不实现居民门户）

Stage G 只落地身份与业务关系，不新增居民页面。后续居民端应通过服务端 `ActorContext(accountId, personId, roles)` 加上 Ownership / Residence 推导范围，而不是由前端传入角色或人员编号。

| 身份关系 | 未来可见模块 |
| --- | --- |
| Owner Account | 我的产权、名下房产、出租挂牌、关联账单、物业通知 |
| Tenant Account | 我的当前住所、与该住所关联的账单、物业通知 |
| Owner + Resident | 两类模块的并集；产权与当前居住关系需明确区分 |
| Member Resident | 当前住所、该住所通知与授权范围内账单；不自动获得产权管理能力 |

后续接口必须由 JWT/`SecurityContext` 解析身份，再从数据库关系判断对象范围。禁止采用 `?personId=...`、请求体 `role=OWNER` 等方式让浏览器决定自身身份。
