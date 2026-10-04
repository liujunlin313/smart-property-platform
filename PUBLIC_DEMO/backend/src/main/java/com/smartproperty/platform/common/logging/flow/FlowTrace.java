package com.smartproperty.platform.common.logging.flow;

import java.util.ArrayList;
import java.util.List;

/** Per-request, in-memory flow outline. It never stores request bodies or secrets. */
public final class FlowTrace {
    private static final ThreadLocal<FlowRecord> CURRENT = new ThreadLocal<>();

    private FlowTrace() { }

    public static void begin(String method, String uri) { CURRENT.set(new FlowRecord(method, uri)); }
    public static FlowRecord current() { return CURRENT.get(); }
    public static FlowRecord complete() { FlowRecord record = CURRENT.get(); CURRENT.remove(); return record; }
    public static void controller(String signature) { if (CURRENT.get() != null) CURRENT.get().controller = signature; }
    public static void service(String signature) { if (CURRENT.get() != null) CURRENT.get().services.add(signature); }
    public static void mapper(String statement, String sql, long durationMs, String outcome) {
        if (CURRENT.get() != null) CURRENT.get().mappers.add(new MapperCall(statement, sql, durationMs, outcome));
    }

    public static final class FlowRecord {
        private final String method;
        private final String uri;
        private final long startedAt = System.nanoTime();
        private String controller;
        private final List<String> services = new ArrayList<>();
        private final List<MapperCall> mappers = new ArrayList<>();
        private FlowRecord(String method, String uri) { this.method = method; this.uri = uri; }
        public String method() { return method; }
        public String uri() { return uri; }
        public long durationMs() { return (System.nanoTime() - startedAt) / 1_000_000L; }
        public String controller() { return controller; }
        public List<String> services() { return List.copyOf(services); }
        public List<MapperCall> mappers() { return List.copyOf(mappers); }
    }

    public record MapperCall(String statement, String sql, long durationMs, String outcome) { }
}
