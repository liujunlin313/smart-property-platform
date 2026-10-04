# GitHub 发布准备审计

日期：2026-10-04。审计、复制与脱敏已完成，尚未创建仓库或上传。原工程只读；公开目录内没有真实运行凭据、原始日志或数据目录。本次不宣称生产安全认证。

## 1. 两版本现状

原始目录为 `C:/Projects/数据库课程设计/多小区智慧物业管理系统_MERGED_V1` 和 `C:/Projects/数据库课程设计/多小区智慧物业管理系统_PUBLIC_DEMO`。两者根目录及其父级均无 .git，branch/HEAD/remote/tracked/untracked/ignored 的 Git 分类均不适用。未发现可审计的 Git commit 历史，不能据此保证更早存在的外部仓库历史已脱敏。

MERGED_V1 共 15,089 文件、383,551,946 字节；PUBLIC_DEMO 共 15,616 文件、612,664,376 字节（包含缓存、构建和材料）。二者均为 backend、frontend、database、deploy、docs、logs 等完整目录。PUBLIC_DEMO 属于完整副本加部署、安全与观测增量；具体代码比对见 docs/VERSION_RELATION.md、docs/SOURCE_COMPARISON.md。没有证据表明两者通过 Git branch 关联。

## 2. 敏感信息审计

扫描原始业务、配置、脚本、SQL、文档文本，并单独读取 logs 下 874 个文本文件。所有候选只记录文件、行与类型，没有在报告显示密钥或密码。用户名字段、PasswordEncoder、token 变量、拒绝日志的关键字等正常代码不作为凭据泄漏。关键词扫描不能证明二进制截图已脱敏，因此该类材料不复制。

| 原工程版本/文件 | 行 | 类型 | 等级 | 副本处理/建议 |
| --- | ---: | --- | --- | --- |
| MERGED_V1/frontend/.env.local | 1 | VITE_AMAP_JS_KEY | HIGH | 本地配置未复制；安全 example 需自行填写 |
| MERGED_V1/frontend/.env.local | 2 | VITE_AMAP_JS_SECURITY_CODE | HIGH | 本地配置未复制；安全 example 需自行填写 |
| MERGED_V1/deploy/local.env.properties | 9 | DB_PASSWORD | HIGH | 本地配置未复制；安全 example 需自行填写 |
| MERGED_V1/deploy/local.env.properties | 13 | REDIS_PASSWORD | HIGH | 本地配置未复制；安全 example 需自行填写 |
| MERGED_V1/deploy/local.env.properties | 19 | MINIO_ROOT_USER | HIGH | 本地配置未复制；安全 example 需自行填写 |
| MERGED_V1/deploy/local.env.properties | 20 | MINIO_ROOT_PASSWORD | CRITICAL | 本地配置未复制；安全 example 需自行填写 |
| MERGED_V1/deploy/local.env.properties | 21 | MINIO_ACCESS_KEY | HIGH | 本地配置未复制；安全 example 需自行填写 |
| MERGED_V1/deploy/local.env.properties | 22 | MINIO_SECRET_KEY | CRITICAL | 本地配置未复制；安全 example 需自行填写 |
| MERGED_V1/deploy/local.env.properties | 27 | APP_BOOTSTRAP_ADMIN_PASSWORD | HIGH | 本地配置未复制；安全 example 需自行填写 |
| MERGED_V1/deploy/local.env.properties | 29 | APP_BOOTSTRAP_VIEWER_PASSWORD | HIGH | 本地配置未复制；安全 example 需自行填写 |
| MERGED_V1/deploy/local.env.properties | 35 | AMAP_WEB_SERVICE_KEY | HIGH | 本地配置未复制；安全 example 需自行填写 |
| MERGED_V1/deploy/local.env.properties | 36 | AMAP_WEB_SERVICE_PRIVATE_KEY | CRITICAL | 本地配置未复制；安全 example 需自行填写 |
| MERGED_V1/deploy/local.env.ps1 | 6 | DB_PASSWORD | HIGH | 本地配置未复制；安全 example 需自行填写 |
| MERGED_V1/deploy/local.env.ps1 | 9 | MINIO_ACCESS_KEY | HIGH | 本地配置未复制；安全 example 需自行填写 |
| MERGED_V1/deploy/local.env.ps1 | 10 | MINIO_SECRET_KEY | CRITICAL | 本地配置未复制；安全 example 需自行填写 |
| MERGED_V1/deploy/local.env.ps1 | 15 | APP_BOOTSTRAP_ADMIN_PASSWORD | HIGH | 本地配置未复制；安全 example 需自行填写 |
| MERGED_V1/deploy/local.env.ps1 | 17 | APP_BOOTSTRAP_VIEWER_PASSWORD | HIGH | 本地配置未复制；安全 example 需自行填写 |
| MERGED_V1/backend/src/main/java/com/smartproperty/platform/auth/provision/service/impl/DemoAccountProvisionServiceImpl.java | 25 | 固定演示初始口令 | MEDIUM | 保留公开演示行为；只用于隔离环境，不能沿用为实际公网账号 |
| PUBLIC_DEMO/.env.docker | 10 | MYSQL_APP_PASSWORD | HIGH | 本地配置未复制；安全 example 需自行填写 |
| PUBLIC_DEMO/.env.docker | 11 | MYSQL_ROOT_PASSWORD | CRITICAL | 本地配置未复制；安全 example 需自行填写 |
| PUBLIC_DEMO/.env.docker | 13 | MINIO_ROOT_USER | HIGH | 本地配置未复制；安全 example 需自行填写 |
| PUBLIC_DEMO/.env.docker | 14 | MINIO_ROOT_PASSWORD | CRITICAL | 本地配置未复制；安全 example 需自行填写 |
| PUBLIC_DEMO/.env.docker | 20 | AMAP_WEB_SERVICE_KEY | HIGH | 本地配置未复制；安全 example 需自行填写 |
| PUBLIC_DEMO/.env.docker | 21 | AMAP_WEB_SERVICE_PRIVATE_KEY | CRITICAL | 本地配置未复制；安全 example 需自行填写 |
| PUBLIC_DEMO/.env.docker | 22 | VITE_AMAP_JS_KEY | HIGH | 本地配置未复制；安全 example 需自行填写 |
| PUBLIC_DEMO/.env.docker | 23 | VITE_AMAP_JS_SECURITY_CODE | HIGH | 本地配置未复制；安全 example 需自行填写 |
| PUBLIC_DEMO/.env.docker | 29 | LOAD_TEST_PASSWORD | HIGH | 本地配置未复制；安全 example 需自行填写 |
| PUBLIC_DEMO/frontend/.env.local | 1 | VITE_AMAP_JS_KEY | HIGH | 本地配置未复制；安全 example 需自行填写 |
| PUBLIC_DEMO/frontend/.env.local | 2 | VITE_AMAP_JS_SECURITY_CODE | HIGH | 本地配置未复制；安全 example 需自行填写 |
| PUBLIC_DEMO/deploy/local.env.properties | 9 | DB_PASSWORD | HIGH | 本地配置未复制；安全 example 需自行填写 |
| PUBLIC_DEMO/deploy/local.env.properties | 13 | REDIS_PASSWORD | HIGH | 本地配置未复制；安全 example 需自行填写 |
| PUBLIC_DEMO/deploy/local.env.properties | 19 | MINIO_ROOT_USER | HIGH | 本地配置未复制；安全 example 需自行填写 |
| PUBLIC_DEMO/deploy/local.env.properties | 20 | MINIO_ROOT_PASSWORD | CRITICAL | 本地配置未复制；安全 example 需自行填写 |
| PUBLIC_DEMO/deploy/local.env.properties | 21 | MINIO_ACCESS_KEY | HIGH | 本地配置未复制；安全 example 需自行填写 |
| PUBLIC_DEMO/deploy/local.env.properties | 22 | MINIO_SECRET_KEY | CRITICAL | 本地配置未复制；安全 example 需自行填写 |
| PUBLIC_DEMO/deploy/local.env.properties | 27 | APP_BOOTSTRAP_ADMIN_PASSWORD | HIGH | 本地配置未复制；安全 example 需自行填写 |
| PUBLIC_DEMO/deploy/local.env.properties | 29 | APP_BOOTSTRAP_VIEWER_PASSWORD | HIGH | 本地配置未复制；安全 example 需自行填写 |
| PUBLIC_DEMO/deploy/local.env.properties | 35 | AMAP_WEB_SERVICE_KEY | HIGH | 本地配置未复制；安全 example 需自行填写 |
| PUBLIC_DEMO/deploy/local.env.properties | 36 | AMAP_WEB_SERVICE_PRIVATE_KEY | CRITICAL | 本地配置未复制；安全 example 需自行填写 |
| PUBLIC_DEMO/deploy/local.env.ps1 | 6 | DB_PASSWORD | HIGH | 本地配置未复制；安全 example 需自行填写 |
| PUBLIC_DEMO/deploy/local.env.ps1 | 9 | MINIO_ACCESS_KEY | HIGH | 本地配置未复制；安全 example 需自行填写 |
| PUBLIC_DEMO/deploy/local.env.ps1 | 10 | MINIO_SECRET_KEY | CRITICAL | 本地配置未复制；安全 example 需自行填写 |
| PUBLIC_DEMO/deploy/local.env.ps1 | 15 | APP_BOOTSTRAP_ADMIN_PASSWORD | HIGH | 本地配置未复制；安全 example 需自行填写 |
| PUBLIC_DEMO/deploy/local.env.ps1 | 17 | APP_BOOTSTRAP_VIEWER_PASSWORD | HIGH | 本地配置未复制；安全 example 需自行填写 |
| PUBLIC_DEMO/backend/src/main/java/com/smartproperty/platform/auth/provision/service/impl/DemoAccountProvisionServiceImpl.java | 25 | 固定演示初始口令 | MEDIUM | 保留公开演示行为；只用于隔离环境，不能沿用为实际公网账号 |
| MERGED_V1/START_HERE.md | 6 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| MERGED_V1/START_HERE.md | 75 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| MERGED_V1/START_HERE.md | 99 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| MERGED_V1/database/DATABASE_V1_GUIDE.md | 20 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| MERGED_V1/database/evidence-phase2a/01_backend_build.txt | 17 | 高熵十六进制值候选，可能是构建标识或凭据 | HIGH | 原始 evidence 全部不复制，不将候选误判为确证秘密 |
| MERGED_V1/database/evidence-phase2a/01_backend_build.txt | 17 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| MERGED_V1/database/evidence-phase2a/01_backend_build.txt | 26 | 高熵十六进制值候选，可能是构建标识或凭据 | HIGH | 原始 evidence 全部不复制，不将候选误判为确证秘密 |
| MERGED_V1/database/evidence-phase2a/01_backend_build.txt | 26 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| MERGED_V1/database/evidence-phase2a/01_backend_build.txt | 29 | 高熵十六进制值候选，可能是构建标识或凭据 | HIGH | 原始 evidence 全部不复制，不将候选误判为确证秘密 |
| MERGED_V1/database/evidence-phase2a/01_backend_build.txt | 29 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| MERGED_V1/database/evidence-phase2a/01_backend_build.txt | 30 | 高熵十六进制值候选，可能是构建标识或凭据 | HIGH | 原始 evidence 全部不复制，不将候选误判为确证秘密 |
| MERGED_V1/database/evidence-phase2a/01_backend_build.txt | 30 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| MERGED_V1/database/evidence-phase2a/05_backend_stdout.txt | 11 | 高熵十六进制值候选，可能是构建标识或凭据 | HIGH | 原始 evidence 全部不复制，不将候选误判为确证秘密 |
| MERGED_V1/database/evidence-phase2a/05_backend_stdout.txt | 11 | 高熵十六进制值候选，可能是构建标识或凭据 | HIGH | 原始 evidence 全部不复制，不将候选误判为确证秘密 |
| MERGED_V1/database/evidence-phase2a/05_backend_stdout.txt | 11 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| MERGED_V1/database/evidence-phase2a/05_backend_stdout.txt | 11 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| MERGED_V1/database/sample-data/01_demo_data.sql | 68 | 合成数据特征：DEMO-ID/测试号码 | SAFE | 明确标注演示/测试；原 SQL 保留 |
| MERGED_V1/database/sample-data/01_demo_data.sql | 69 | 合成数据特征：DEMO-ID/测试号码 | SAFE | 明确标注演示/测试；原 SQL 保留 |
| MERGED_V1/database/sample-data/01_demo_data.sql | 70 | 合成数据特征：DEMO-ID/测试号码 | SAFE | 明确标注演示/测试；原 SQL 保留 |
| MERGED_V1/database/sample-data/01_demo_data.sql | 71 | 合成数据特征：DEMO-ID/测试号码 | SAFE | 明确标注演示/测试；原 SQL 保留 |
| MERGED_V1/database/sample-data/01_demo_data.sql | 72 | 合成数据特征：DEMO-ID/测试号码 | SAFE | 明确标注演示/测试；原 SQL 保留 |
| MERGED_V1/database/sample-data/01_demo_data.sql | 73 | 合成数据特征：DEMO-ID/测试号码 | SAFE | 明确标注演示/测试；原 SQL 保留 |
| MERGED_V1/database/scripts/verify-phase2a.ps1 | 39 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| MERGED_V1/database/tests/02_positive_cases.sql | 32 | 合成数据特征：DEMO-ID/测试号码 | SAFE | 明确标注演示/测试；原 SQL 保留 |
| MERGED_V1/deploy/build-local.ps1 | 11 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| MERGED_V1/deploy/local.env.properties | 14 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| MERGED_V1/deploy/local.env.properties | 15 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| MERGED_V1/deploy/local.env.properties | 24 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| MERGED_V1/deploy/local.env.ps1 | 12 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| MERGED_V1/deploy/start-backend-dev.ps1 | 35 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| MERGED_V1/docs/PPT_ER_MODEL_AUDIT.md | 3 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| MERGED_V1/docs/PPT_FINAL_STATE_AUDIT.md | 3 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| MERGED_V1/docs/PPT_TRANSACTION_ROLLBACK_EVIDENCE.md | 39 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| MERGED_V1/docs/PRE_STAGE1_RECOVERY_REPORT.md | 4 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| MERGED_V1/docs/PRE_STAGE1_RECOVERY_REPORT.md | 5 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| MERGED_V1/docs/PUBLIC_DEPLOYMENT_PRECHECK.md | 64 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| MERGED_V1/docs/STAGE_B_CORE_BUSINESS_DEVELOPMENT_REPORT.md | 5 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| MERGED_V1/docs/testing/TEST_CHECKPOINT.md | 4 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/.env.docker | 30 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/compose.yaml | 100 | 固定内网地址 | MEDIUM | Compose CORS 改环境变量；文档改 LAN_HOST；日志排除 |
| PUBLIC_DEMO/compose.yaml | 100 | 固定内网地址 | MEDIUM | Compose CORS 改环境变量；文档改 LAN_HOST；日志排除 |
| PUBLIC_DEMO/START_HERE.md | 6 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/START_HERE.md | 75 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/START_HERE.md | 99 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/database/DATABASE_V1_GUIDE.md | 20 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/database/evidence-phase2a/01_backend_build.txt | 17 | 高熵十六进制值候选，可能是构建标识或凭据 | HIGH | 原始 evidence 全部不复制，不将候选误判为确证秘密 |
| PUBLIC_DEMO/database/evidence-phase2a/01_backend_build.txt | 17 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/database/evidence-phase2a/01_backend_build.txt | 26 | 高熵十六进制值候选，可能是构建标识或凭据 | HIGH | 原始 evidence 全部不复制，不将候选误判为确证秘密 |
| PUBLIC_DEMO/database/evidence-phase2a/01_backend_build.txt | 26 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/database/evidence-phase2a/01_backend_build.txt | 29 | 高熵十六进制值候选，可能是构建标识或凭据 | HIGH | 原始 evidence 全部不复制，不将候选误判为确证秘密 |
| PUBLIC_DEMO/database/evidence-phase2a/01_backend_build.txt | 29 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/database/evidence-phase2a/01_backend_build.txt | 30 | 高熵十六进制值候选，可能是构建标识或凭据 | HIGH | 原始 evidence 全部不复制，不将候选误判为确证秘密 |
| PUBLIC_DEMO/database/evidence-phase2a/01_backend_build.txt | 30 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/database/evidence-phase2a/05_backend_stdout.txt | 11 | 高熵十六进制值候选，可能是构建标识或凭据 | HIGH | 原始 evidence 全部不复制，不将候选误判为确证秘密 |
| PUBLIC_DEMO/database/evidence-phase2a/05_backend_stdout.txt | 11 | 高熵十六进制值候选，可能是构建标识或凭据 | HIGH | 原始 evidence 全部不复制，不将候选误判为确证秘密 |
| PUBLIC_DEMO/database/evidence-phase2a/05_backend_stdout.txt | 11 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/database/evidence-phase2a/05_backend_stdout.txt | 11 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/database/sample-data/01_demo_data.sql | 68 | 合成数据特征：DEMO-ID/测试号码 | SAFE | 明确标注演示/测试；原 SQL 保留 |
| PUBLIC_DEMO/database/sample-data/01_demo_data.sql | 69 | 合成数据特征：DEMO-ID/测试号码 | SAFE | 明确标注演示/测试；原 SQL 保留 |
| PUBLIC_DEMO/database/sample-data/01_demo_data.sql | 70 | 合成数据特征：DEMO-ID/测试号码 | SAFE | 明确标注演示/测试；原 SQL 保留 |
| PUBLIC_DEMO/database/sample-data/01_demo_data.sql | 71 | 合成数据特征：DEMO-ID/测试号码 | SAFE | 明确标注演示/测试；原 SQL 保留 |
| PUBLIC_DEMO/database/sample-data/01_demo_data.sql | 72 | 合成数据特征：DEMO-ID/测试号码 | SAFE | 明确标注演示/测试；原 SQL 保留 |
| PUBLIC_DEMO/database/sample-data/01_demo_data.sql | 73 | 合成数据特征：DEMO-ID/测试号码 | SAFE | 明确标注演示/测试；原 SQL 保留 |
| PUBLIC_DEMO/database/scripts/verify-phase2a.ps1 | 39 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/database/tests/02_positive_cases.sql | 32 | 合成数据特征：DEMO-ID/测试号码 | SAFE | 明确标注演示/测试；原 SQL 保留 |
| PUBLIC_DEMO/deploy/build-local.ps1 | 11 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/deploy/local.env.properties | 14 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/deploy/local.env.properties | 15 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/deploy/local.env.properties | 24 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/deploy/local.env.ps1 | 12 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/deploy/start-backend-dev.ps1 | 35 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/docs/DOCKER_STAGE1_RUNBOOK.md | 48 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/docs/DOCKER_STAGE1_RUNBOOK.md | 84 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/docs/DOCKER_STAGE1_RUNBOOK.md | 94 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/docs/DOCKER_STAGE1_RUNBOOK.md | 142 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/docs/DOCKER_STAGE2_5_OBSERVABILITY_RUNBOOK.md | 7 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/docs/DOCKER_STAGE2_5_OBSERVABILITY_RUNBOOK.md | 91 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/docs/PPT_ER_MODEL_AUDIT.md | 3 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/docs/PPT_FINAL_STATE_AUDIT.md | 3 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/docs/PPT_TRANSACTION_ROLLBACK_EVIDENCE.md | 39 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/docs/PRE_STAGE1_RECOVERY_REPORT.md | 4 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/docs/PRE_STAGE1_RECOVERY_REPORT.md | 5 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/docs/PUBLIC_DEPLOYMENT_PRECHECK.md | 64 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/docs/STAGE_B_CORE_BUSINESS_DEVELOPMENT_REPORT.md | 5 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/docs/report-archaeology/PROJECT_REPORT_FACT_BASELINE.md | 18 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/docs/report-archaeology/REPORT_EVIDENCE_MAP.md | 3 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/docs/report-archaeology/REPORT_EVIDENCE_MAP.md | 12 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/docs/report-archaeology/REPORT_FULL_STRUCTURE_DRAFT_V1.md | 3 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/docs/report-archaeology/REPORT_FULL_STRUCTURE_DRAFT_V1.md | 3 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/docs/report-archaeology/REPORT_FULL_STRUCTURE_DRAFT_V3.md | 701 | 固定内网地址 | MEDIUM | Compose CORS 改环境变量；文档改 LAN_HOST；日志排除 |
| PUBLIC_DEMO/docs/report-archaeology/REPORT_FULL_STRUCTURE_DRAFT_V3.md | 701 | 固定内网地址 | MEDIUM | Compose CORS 改环境变量；文档改 LAN_HOST；日志排除 |
| PUBLIC_DEMO/docs/report-archaeology/REPORT_FULL_STRUCTURE_DRAFT_V3.md | 886 | 固定内网地址 | MEDIUM | Compose CORS 改环境变量；文档改 LAN_HOST；日志排除 |
| PUBLIC_DEMO/docs/report-archaeology/REPORT_FULL_STRUCTURE_DRAFT_V3.md | 886 | 固定内网地址 | MEDIUM | Compose CORS 改环境变量；文档改 LAN_HOST；日志排除 |
| PUBLIC_DEMO/docs/report-archaeology/REPORT_SECOND_PASS_HIGHLIGHT_AUDIT.md | 3 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/docs/report-archaeology/REPORT_SECOND_PASS_HIGHLIGHT_AUDIT.md | 3 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/docs/report-archaeology/REPORT_V3_CHANGELOG.md | 31 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/docs/report-archaeology/REPORT_V3_CHANGELOG.md | 41 | 固定内网地址 | MEDIUM | Compose CORS 改环境变量；文档改 LAN_HOST；日志排除 |
| PUBLIC_DEMO/docs/report-archaeology/REPORT_V3_CHANGELOG.md | 43 | 固定内网地址 | MEDIUM | Compose CORS 改环境变量；文档改 LAN_HOST；日志排除 |
| PUBLIC_DEMO/docs/report-archaeology/REPORT_V3_CHANGELOG.md | 49 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |
| PUBLIC_DEMO/docs/testing/TEST_CHECKPOINT.md | 4 | 历史个人/工程绝对路径 | LOW | Markdown 中个人用户目录改示例；源脚本保留现有 PATH 回退与真实结构 |


日志位置另见 docs/LOG_RISK_LOCATIONS.md。未检出准备公开文本中的实际 JWT、私钥文件或口令哈希；在没有 Git 历史的情况下不虚构历史 commit 检测结论。CRITICAL/HIGH 的本地运行凭据均通过不复制处理。固定 demo 初始口令作为公开演示行为保留并单列 MEDIUM 使用限制，没有将其误写成生产安全机制。

## 3. 不应公开的文件

必须排除实际 .env.docker、frontend/.env.local、deploy/local.env.*、原始日志、会话/HTTP dump、数据卷目录及备份。构建产物、node_modules、IDE、Python 缓存、ZIP 和截图/PPT/报告归档建议不进入源码仓库。原始 evidence 不复制。migrations、Mapper XML、tests、源码、合成样例 SQL、Runbook 和安全 example 可公开。详细计数见 docs/EXCLUDED_FILES.md。

本次两个工程内没有提供待公开的完整最终数据库 SQL dump；历史备份引用不代表备份被复制。数据库备份、MinIO 数据及部署备份位于别处时，本次没有读取或搬运它们。

## 4. 已完成的安全化整理

按一个仓库、两个目录复制，跳过明确不应公开的文件；仅改副本的说明/配置。PUBLIC_DEMO Compose 的固定 LAN CORS 列表改为 APP_ALLOWED_ORIGINS 环境变量，默认 localhost/loopback；示例补 PUBLIC_DEMO_ACTIVE_RUN_LOG_ROOT，使完整 Compose 必需变量可解析。文档个人路径用示例替换。详见 docs/PREP_CHANGELOG.md；业务源码的 SHA-256 一致检查通过。

## 5. .gitignore

新增根 .gitignore，同时保留两个版本原规则；覆盖 node_modules、target、dist、日志、IDE、实际 env/local.env、缓存、运行数据、备份和归档，允许 .env.example 与 .env.docker.example。不忽略全部 SQL，不移除 migrations/tests。没有初始化 .git，因此未虚构 git check-ignore/staged 校验结果；所有需要排除的内容已经物理未复制。

## 6. 示例配置

保留两版 deploy/local.env.example.properties、local.env.example.ps1、database/flyway/flyway.conf.example；新增各自 frontend/.env.example。PUBLIC_DEMO/.env.docker.example 已补齐日志/CORS 变量并通过 compose config --quiet。使用者复制示例到被忽略的本地文件后填自己的 MySQL、MinIO、JWT、高德凭据。没有复制现有真实值，也没有修改原配置或重新生成原密钥。

## 7. 推荐仓库结构

采用一个 repository 下 MERGED_V1 与 PUBLIC_DEMO 两个并列目录。教师无需切换 branch，能直观看到本地开发和 Docker 演示成果。源码重复是已接受的展示成本。本轮不抽 shared、不改 monorepo 构建、不重排业务目录。分支方案会增加教师比较成本，两个独立仓库会拆散课程成果；当前没有原 Git 历史必须继承的技术障碍，因此没有理由偏离用户确定方案。

## 8. README 与运行验证

根 README 明确版本、实际依赖、数据库边界、两套启动方式、安全配置与状态；PUBLIC_DEMO 保留 frontend/，没有为了示意图改成 web/。数据库最终快照未公开，故不能承诺 clone 后一键登录。

两版 Java compile/package PASS；选定 mock 单元测试每版 3 用例 PASS；两版 Vite build PASS；V1—V10 全部保留且内容一致；Docker Compose 示例 config PASS。Dockerfile 的 build context/COPY 路径静态检查 PASS。没有新建容器、修改 named volumes、运行数据库 SQL 或长 E2E。验证细节、工具和警告见 docs/VALIDATION_RESULTS.md。

## 9. 尚需用户了解的边界

迁移来源缺口未补造；最终快照/对象数据仍不公开；没有 LICENSE 选择。原始 Git 历史不可用，其他曾经上传的副本不属于本次核验范围。固定 demo 口令和 historical 工具绝对路径保留并披露，代码不作安全重构。部分历史证据链接失效。Compose 固定 named volume/network 名可能与当前本机部署相同，启动前需核对，不能因目录不同就假定数据隔离。未执行真实运行回归，不将静态与构建检查夸大为完整系统新环境验收。

## 10. 由用户本人执行的 Git 步骤

以下仅供本人检查后手动执行，本轮未执行：

```powershell
cd <github-prep-folder>
git init -b main
git status --short
git add README.md .gitignore docs GITHUB_PUBLICATION_AUDIT.md ORIGINAL_FILES_UNTOUCHED_CHECK.md MERGED_V1 PUBLIC_DEMO
git diff --cached --stat
git diff --cached
# 逐项确认没有实际配置、密钥、数据或日志后
git commit -m "Publish database course project snapshots"
# 本人去 GitHub 创建空仓库，随后替换下面的示例 URL
git remote add origin https://github.com/<your-account>/<your-repository>.git
git remote -v
git push -u origin main
# 如需要标记已检查的课程成果快照
git tag -a course-design-publication-v1 -m "Reviewed course project snapshots"
git push origin course-design-publication-v1
```

不要把本地原目录或 publication_private_work/pv 验证工作目录一起上传。Git 命令针对最终 github-prep 根目录，准备副本未携带任何原 .git。

## 11. 真正发布后同步报告

只有本人确认公开 URL、commit/tag 后，再更新正式报告 8.5.2 尚未发布表述、附录 B 材料索引及源码获取说明；两版对应同一提交中的不同目录，不伪称它们来自不同 branch。随后按实际内容更新 Word 目录与页码。本轮没有修改报告或声称已发布。
