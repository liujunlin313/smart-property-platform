# 数据库初始化边界

两个目录都保留 V1—V10、reference seed、sample-data 和 tests。Migration 原文件内容与原工程完全一致，本轮未补造迁移、重放 SQL 或访问数据库。

V1—V10 与最终历史演示数据库存在已知结构来源缺口。部署历史采用恢复最终快照的方式；公开副本不附含账号、口令哈希、会话和业务数据的原始备份，也不附 MinIO 对象数据。README 因此不承诺一键启动即可得到最终系统。

如果需要重建课程环境，应先在独立 MySQL 实例或明确授权的新数据库中验证迁移链，并核对最终目标结构、View、索引及业务表。sample-data/01_demo_data.sql 的原注释说明它面向迁移至 V2 的演示样例，不能未经检查当作最终 V10 全量 seed。它引用的 file object 不随 SQL 自动出现。

读取工程和执行 mock 单元测试不需要历史业务数据库。SQL positive/negative tests、数据准备脚本、角色分配、入住、缴费、通知以及压力测试可能写入数据；不要用它们验证长期演示库的“只读可用性”。

Docker .env.docker 中 APP_FLYWAY_ENABLED 以及 compose 运行配置保持原有冻结策略。空数据卷的 MySQL 初始化仅创建基础数据库与数据库账号；不会生成最终业务 Schema。Named volume 持久化不替代备份，删除卷会丢失数据。
