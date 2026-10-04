# 发布副本验证结果

日期：2026-10-04。构建均使用独立验证副本，公开目录没有 target、dist 或 node_modules。本次没有启动后端、容器、Redis、MinIO 或数据库，也没有运行会写入长期数据库的 smoke/E2E/负向 SQL/压力测试。

| 检查 | MERGED_V1 | PUBLIC_DEMO |
| --- | --- | --- |
| Maven 编译与 package | PASS | PASS |
| ChargeNoticeComposerTest、NotificationServiceImplTest | PASS，3 个用例，0 失败 | PASS，3 个用例，0 失败 |
| Vue/Vite production build | PASS | PASS |
| V1—V10 保留且内容哈希一致 | PASS | PASS |
| Docker Compose 示例 config --quiet | 不适用 | PASS |
| Dockerfile COPY 路径和 build context 静态核对 | 不适用 | PASS |

验证环境为 JDK 21.0.5、Maven 3.9.12；工程规定 Java 21，Docker 构建镜像规定 Maven 3.9.9，未据此改版本。前端使用两版原先安装依赖的独立复制并核对 package-lock 中版本；不是本次 fresh npm ci 下载验证。Vite 7.3.6 构建出现第三方 PURE 注释和大 chunk 警告，不影响构建成功。Mockito 的动态 agent 提示不影响三个 mock 单元用例。

初次沙箱构建出现访问拒绝以及离线父 POM 无法读取；在同一验证副本中使用授权的本机缓存后通过，未修改 pom 或源代码。Compose config 使用 .env.docker.example，未打印配置展开值，未使用真实 .env。

835 个公开业务源文件/迁移/测试 SQL 与各自原始文件逐项 SHA-256 相同。公开副本只改文档、配置与排除文件。构建通过不等价于最终历史数据库可从迁移链无缺口重建，也不证明完整运行回归已重新执行。
