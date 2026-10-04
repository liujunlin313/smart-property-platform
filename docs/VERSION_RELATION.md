# 两个成果版本的关系

MERGED_V1 是本地稳定开发工程；PUBLIC_DEMO 是在该工程基础上保留业务并扩展容器部署、日志、安全与访问控制配置的完整副本。两个目录并列保留，未合并、抽模块、改包名或重建 Git 历史。

按本次原始文件内容哈希比较：backend/src 有 376 个同路径文件相同、17 个不同，PUBLIC_DEMO 另有 5 个文件；frontend/src 的 52 个文件全部相同。database 的 46 个同路径文件全部相同（其中包含未公开的历史 evidence），deploy 的 112 个同路径文件全部相同。PUBLIC_DEMO 的 scripts 另有 21 个文件，其中有未公开的 Python 缓存。

容器版独有 backend/frontend Dockerfile、compose.yaml、Nginx 配置，以及 scripts/docker、scripts/loadtest 和 scripts/observability。它的 Java 差异包含日志运行身份、SQL/HTTP 观测、安全事件、请求限流与鉴权相关实现，因此不能把它描述成完全相同源码仅换了启动命令。完整逐文件差异见 SOURCE_COMPARISON.md。

两版分别保留 MySQL community_db 配置，但本地版连接 Windows localhost，容器版连接 mysql 服务 DNS。原 Docker 运行用 named volume，与原 Windows 数据目录分开。发布副本不含两边的真实数据及密钥。

教师阅读数据库设计与本地工程时查看 MERGED_V1；阅读五容器、Nginx 单入口和容器持久化方案时查看 PUBLIC_DEMO。长期维护如需同步，应显式比较修改，本轮没有建立自动同步机制。
