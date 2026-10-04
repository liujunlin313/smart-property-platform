package com.smartproperty.platform.common.logging.flow;

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

/** Logs mapper identity and compact SQL templates without expanding business parameters. */
@Component
@Intercepts({
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class FlowMybatisInterceptor implements Interceptor {
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
        long startedAt = System.nanoTime();
        try {
            Object result = invocation.proceed();
            String outcome = result instanceof java.util.Collection<?> rows ? "rows=" + rows.size()
                    : result instanceof Number affected ? "affected=" + affected : "result=object";
            FlowTrace.mapper(shortStatement(statement.getId()), compactSql(boundSql.getSql()),
                    (System.nanoTime() - startedAt) / 1_000_000L, outcome);
            return result;
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
        int max = properties.safeSqlMaxLength();
        return compact.length() > max ? compact.substring(0, max - 1) + "…" : compact;
    }
}
