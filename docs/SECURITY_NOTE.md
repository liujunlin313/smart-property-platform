# 公开副本的安全边界

发布副本没有真实 .env.docker、frontend/.env.local 或 deploy/local.env 配置，不附原始运行日志、数据库/MinIO 数据、备份、报告/PPT/截图归档。高德 Web Service Key、签名私钥、JS Key 与 securityJsCode、JWT、数据库及对象存储密码均须由使用者填写到被忽略的本地配置。

高德 JS Key 按平台规则出现在浏览器请求中是其运行方式，不等于应把本项目真实 Key 提交仓库。请配置自己的配套凭据并使用相应平台限制。本轮没有远端上传或凭据旋转。既往聊天中提供过的真实凭据如有其他外部暴露，应由所有者自行评估轮换。

原始日志可能有用户、请求体、IP、路径和认证信息；静态关键字命中不等于每一处都泄密。本次直接不复制原始 evidence/logs，保留安全的 Markdown 说明及 SQL 测试源码。二进制截图和报告不能通过文本扫描证明无个人信息，故不纳入这个公开副本。

database/sample-data/01_demo_data.sql 明确标注 demonstration data，人员身份证使用 DEMO-ID，占位手机集中使用演示编号；database/tests/02_positive_cases.sql 为测试值。保留它们用于教学，不能把它们或其地址解释为真实居民数据。最终历史数据库备份没有公开，也未从备份生成新的脱敏 dump。

DemoAccountProvisionServiceImpl 中保留了历史演示账号固定初始口令。它属于公开的演示设计，不是从本机配置泄漏的私密口令，但安全性较弱；只能用于隔离课程演示，不能将其生成的账号沿用到真实公网环境。本轮未改变其业务代码。日志访问控制、Cookie/CORS 与限流不是对公网生产安全的完整保证。

原始源码和脚本中的历史个人安装路径按低风险环境信息记录；脚本保留已有 PATH 回退。不要按这些路径安装工具。根 README 给出普通工具命令。复用 Docker Compose 时须先核对固定 volume/network 名，避免误连接既有数据卷。

开源 LICENSE 尚未选择。本副本未携带 .git，因此没有将原历史内容带入新仓库。上传前仍应由所有者检查 git diff --cached，确认暂存区只有准备公开的文件。
