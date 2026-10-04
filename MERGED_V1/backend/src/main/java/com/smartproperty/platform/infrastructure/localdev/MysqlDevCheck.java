package com.smartproperty.platform.infrastructure.localdev;

import java.util.List;

final class MysqlDevCheck {
    private final DevScriptRunner runner;

    MysqlDevCheck(DevScriptRunner runner) {
        this.runner = runner;
    }

    void verify(LocalRunContext context) {
        DevScriptRunner.ScriptResult result = runner.run(
                context.configuration(), "check-mysql.ps1", List.of());
        if (result.exitCode() != 0) {
            throw new IllegalStateException("MySQL/community_db preflight failed: " + result.lastOutput());
        }
        DevConsole.ok("MySQL community_db is available");
    }
}
