package com.smartproperty.platform.common.logging.flow;

import com.smartproperty.platform.common.logging.ObservabilityLog;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Logs mapper identity and compact SQL templates without expanding business parameters. */
@Component
@Intercepts({
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class FlowMybatisInterceptor implements Interceptor {
    private static final Object TRANSACTION_TELEMETRY_KEY = new Object();
    private static final Pattern OPERATION = Pattern.compile("^\\s*(SELECT|INSERT|UPDATE|DELETE)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern TABLE = Pattern.compile("(?i)\\b(?:FROM|JOIN|UPDATE|INTO)\\s+[`]?([a-zA-Z0-9_]+)[`]?");
    private static final Pattern UPDATE_SET = Pattern.compile("(?i)\\bUPDATE\\s+[`]?[a-zA-Z0-9_]+[`]?\\s+SET\\s+(.+?)\\s+WHERE\\b");
    private final FlowLoggingProperties properties;

    public FlowMybatisInterceptor(FlowLoggingProperties properties) {
        this.properties = properties;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (!properties.flowEnabled() || FlowTrace.current() == null) return invocation.proceed();
        Object[] arguments = invocation.getArgs();
        MappedStatement statement = (MappedStatement) arguments[0];
        Object parameter = arguments.length > 1 ? arguments[1] : null;
        BoundSql boundSql = statement.getBoundSql(parameter);
        String sql = compactSql(boundSql.getSql());
        String operation = operationOf(sql);
        List<String> tables = tablesOf(sql);
        List<String> changedFields = changedFieldsOf(sql, operation);
        boolean transactionActive = TransactionSynchronizationManager.isActualTransactionActive();
        registerTransactionCompletionIfNecessary(transactionActive);
        long startedAt = System.nanoTime();
        try {
            Object result = invocation.proceed();
            String outcome = result instanceof Collection<?> rows ? "rows=" + rows.size()
                    : result instanceof Number affected ? "affected=" + affected : "result=object";
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000L;
            String statementId = shortStatement(statement.getId());
            FlowTrace.mapper(statementId, sql, durationMs, outcome);
            var fields = ObservabilityLog.event("MAPPER_CALL");
            fields.put("mapped_statement_id", statementId);
            fields.put("operation", invocation.getMethod().getName().toUpperCase());
            fields.put("duration_ms", durationMs);
            fields.put("outcome", outcome);
            fields.put("sql_template", sql);
            ObservabilityLog.write(ObservabilityLog.TRACE, fields);
            writeDatabaseOperation(statementId, operation, tables, changedFields, result, durationMs, transactionActive, "SUCCESS");
            return result;
        } catch (Throwable exception) {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000L;
            var fields = ObservabilityLog.event("MAPPER_CALL");
            fields.put("mapped_statement_id", shortStatement(statement.getId()));
            fields.put("operation", operation);
            fields.put("duration_ms", durationMs);
            fields.put("outcome", "FAILURE");
            fields.put("sql_template", sql);
            ObservabilityLog.write(ObservabilityLog.TRACE, fields);
            writeDatabaseOperation(shortStatement(statement.getId()), operation, tables, changedFields, null, durationMs, transactionActive, "FAILURE");
            throw exception;
        } finally {
        }
    }

    private String shortStatement(String id) {
        int split = id.lastIndexOf('.');
        String method = split < 0 ? id : id.substring(split + 1);
        int typeSplit = id.lastIndexOf('.', split - 1);
        String type = typeSplit < 0 ? id.substring(0, split) : id.substring(typeSplit + 1, split);
        return type + "." + method;
    }

    private String compactSql(String sql) {
        String compact = sql == null ? "" : sql.replaceAll("\\s+", " ").trim();
        // SQL templates have no expanded parameters, but column names can still reveal token storage internals.
        compact = compact.replaceAll("(?i)(password(_hash)?|refresh_token(_hash)?|token_hash|access_token|secret_key)", "[SENSITIVE_COLUMN]");
        int max = properties.safeSqlMaxLength();
        return compact.length() > max ? compact.substring(0, max - 1) + "…" : compact;
    }

    private void writeDatabaseOperation(String statementId, String operation, List<String> tables,
                                        List<String> changedFields, Object result, long durationMs,
                                        boolean transactionActive, String resultState) {
        var fields = ObservabilityLog.event("DATABASE_OPERATION");
        fields.put("mapped_statement_id", statementId);
        fields.put("operation", operation);
        fields.put("tables", tables);
        fields.put("changed_fields", changedFields);
        fields.put("duration_ms", durationMs);
        fields.put("transaction_active", transactionActive);
        fields.put("result", resultState);
        if (result instanceof Number number) fields.put("affected_rows", number.longValue());
        if (result instanceof Collection<?> rows) fields.put("returned_rows", rows.size());
        ObservabilityLog.write(ObservabilityLog.DATABASE, fields);
    }

    private void registerTransactionCompletionIfNecessary(boolean transactionActive) {
        if (!transactionActive || !TransactionSynchronizationManager.isSynchronizationActive()
                || TransactionSynchronizationManager.getResource(TRANSACTION_TELEMETRY_KEY) != null) return;
        TransactionSynchronizationManager.bindResource(TRANSACTION_TELEMETRY_KEY, Boolean.TRUE);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                var fields = ObservabilityLog.event("DATABASE_TRANSACTION");
                fields.put("transaction_outcome", status == STATUS_COMMITTED ? "COMMIT" : "ROLLBACK");
                ObservabilityLog.write(ObservabilityLog.DATABASE, fields);
                if (TransactionSynchronizationManager.hasResource(TRANSACTION_TELEMETRY_KEY)) {
                    TransactionSynchronizationManager.unbindResource(TRANSACTION_TELEMETRY_KEY);
                }
            }
        });
    }

    private String operationOf(String sql) {
        Matcher matcher = OPERATION.matcher(sql);
        return matcher.find() ? matcher.group(1).toUpperCase(Locale.ROOT) : "OTHER";
    }

    private List<String> tablesOf(String sql) {
        Set<String> tables = new LinkedHashSet<>();
        Matcher matcher = TABLE.matcher(sql);
        while (matcher.find()) {
            String table = matcher.group(1).toLowerCase(Locale.ROOT);
            if (!table.contains("sensitive")) tables.add(table);
        }
        return List.copyOf(tables);
    }

    private List<String> changedFieldsOf(String sql, String operation) {
        if (!"UPDATE".equals(operation)) return List.of();
        Matcher matcher = UPDATE_SET.matcher(sql);
        if (!matcher.find()) return List.of();
        List<String> fields = new ArrayList<>();
        for (String assignment : matcher.group(1).split(",")) {
            String[] pair = assignment.split("=", 2);
            if (pair.length == 0) continue;
            String field = pair[0].replaceAll("[`\\s]", "").toLowerCase(Locale.ROOT);
            if (!field.isBlank() && !field.contains("sensitive")) fields.add(field);
        }
        return List.copyOf(new LinkedHashSet<>(fields));
    }
}
