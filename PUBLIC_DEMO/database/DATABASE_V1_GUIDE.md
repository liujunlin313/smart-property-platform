# Database V1 运行指南

## 1. 目录职责

```text
database/
├── migrations/       # 唯一版本化 DDL/稳定字典 Seed（Flyway Source of Truth）
├── sample-data/      # 可选 Demo 数据，不随生产迁移自动导入
├── tests/            # 可复跑 Schema/正向/负向/JOIN/断言 SQL
├── flyway/           # 独立 Maven Flyway 运行器，不是后端工程
├── scripts/          # 一键隔离验证脚本
└── evidence/         # 实际运行输出
```

## 2. 一键隔离验证（推荐）

在 PowerShell 中执行：

```powershell
Set-Location C:\Projects\数据库课程设计\smart-property-platform-codex-spec-v1\smart-property-platform-codex-spec-v1
.\database\scripts\verify-database-v1.ps1
```

脚本会：

1. 在系统临时目录初始化一个无密码、仅本机可访问的 MySQL 8 测试实例（默认端口 33306）；
2. 创建临时测试库与低权限项目账户；
3. 使用 Flyway Maven Plugin 13.1.0 执行 `V1`、`V2`；
4. 导入 Demo Seed；
5. 执行结构检查、正向测试、16 个预期失败的负向测试、核心 JOIN 与 14 个断言；
6. 把日志写入 `database/evidence/`；
7. 关闭实例并删除经路径校验的临时数据目录。

第一次运行 Maven/Flyway 时需要访问 Maven Central 下载依赖。不会修改机器上已有的 3306 MySQL 实例。

## 3. 在已有 MySQL 上执行

先由管理员创建空库和项目账号（账号密码自行替换且不要提交）：

```sql
CREATE DATABASE smart_property
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'smart_property'@'localhost' IDENTIFIED BY 'replace_me';
GRANT ALL PRIVILEGES ON smart_property.* TO 'smart_property'@'localhost';
```

在 `database/flyway/` 目录运行：

```powershell
mvn `
  -Dflyway.url="jdbc:mysql://127.0.0.1:3306/smart_property?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai" `
  -Dflyway.user=smart_property `
  -Dflyway.password="your_private_password" `
  -Dflyway.locations="filesystem:../migrations" `
  flyway:migrate
```

Demo 数据独立导入，生产环境不要执行：

```powershell
mysql -h 127.0.0.1 -P 3306 -u smart_property -p smart_property `
  -e "SOURCE ../sample-data/01_demo_data.sql"
```

## 4. 重建与回滚

- 从空库恢复：新建空 schema → `flyway:migrate` → 可选导入 Demo Seed。
- V1 尚无破坏性回滚脚本。测试环境需要重建时，只删除明确命名的测试 schema，再从空库迁移。
- 已执行的 `V1__...` / `V2__...` 不得原地修改；未来修复必须新增 V3、V4……。
- 一键脚本只删除它自己在系统临时目录创建且通过前缀校验的 runtime 目录。

## 5. 当前索引范围

V1 仅包含 PK、UNIQUE、FK/明显必要索引。复合索引、覆盖索引、LIKE、空间索引和 EXPLAIN ANALYZE 实验留待数据库优化 Stage。

