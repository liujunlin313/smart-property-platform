package com.smartproperty.platform.common.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartproperty.platform.common.web.RequestTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Small JSONL writer shared by the runtime observability streams. Never pass request bodies or secrets here. */
public final class ObservabilityLog {
    public static final Logger HTTP = LoggerFactory.getLogger("HTTP_JSON");
    public static final Logger TRACE = LoggerFactory.getLogger("FLOW_TRACE");
    public static final Logger AUDIT = LoggerFactory.getLogger("AUDIT_JSON");
    public static final Logger SECURITY = LoggerFactory.getLogger("SECURITY_JSON");
    public static final Logger DATABASE = LoggerFactory.getLogger("DATABASE_JSON");
    private static final ObjectMapper JSON = new ObjectMapper();

    private ObservabilityLog() { }

    public static Map<String, Object> event(String eventType) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("timestamp", OffsetDateTime.now().toString());
        fields.put("event", eventType);
        fields.put("request_id", RequestTrace.currentTraceId());
        fields.put("load_test_id", RequestTrace.currentLoadTestId());
        fields.put("deployment_id", valueOrDash(MDC.get("deploymentId")));
        fields.put("instance_id", valueOrDash(MDC.get("instanceId")));
        return fields;
    }

    public static void write(Logger logger, Map<String, Object> fields) {
        try {
            logger.info(JSON.writeValueAsString(fields));
        } catch (JsonProcessingException exception) {
            logger.warn("{\"event\":\"OBSERVABILITY_SERIALIZATION_FAILURE\",\"request_id\":\"{}\"}",
                    RequestTrace.currentTraceId());
        }
    }

    private static String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
