# 发布副本整理记录

| 版本/文件 | 行 | 原值类型 | 副本值/方式 | 运行影响 |
| --- | --- | --- | --- | --- |
| PUBLIC_DEMO/docs/report-archaeology/REPORT_FULL_STRUCTURE_DRAFT_V3.md | — | 个人路径/固定内网地址 | your-user / LAN_HOST | 仅文档示例 |
| PUBLIC_DEMO/docs/report-archaeology/REPORT_V3_CHANGELOG.md | — | 个人路径/固定内网地址 | your-user / LAN_HOST | 仅文档示例 |
| PUBLIC_DEMO/compose.yaml | 100 | 固定本机/校园网 CORS 列表 | ${APP_ALLOWED_ORIGINS:-...} | 默认 loopback；LAN 时自行填写 |
| PUBLIC_DEMO/.env.docker.example | 新增 | 缺失必需日志变量 | PUBLIC_DEMO_ACTIVE_RUN_LOG_ROOT | 补全示例，不启动服务 |

实际 local.env、.env.docker、frontend/.env.local 没有复制；原有 local.env.example 保留并新增 frontend/.env.example。没有修改业务 Java、Vue、JS、Mapper XML、SQL、迁移、脚本逻辑、pom、lockfile 或 Dockerfile。根 README 和 docs 是新增公开说明。
