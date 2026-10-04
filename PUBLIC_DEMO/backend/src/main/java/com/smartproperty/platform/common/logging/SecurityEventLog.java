package com.smartproperty.platform.common.logging;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/** Records security facts and an optional conservative classification; it never logs credentials or tokens. */
public final class SecurityEventLog {
    private SecurityEventLog() { }

    public static void write(HttpServletRequest request, String event, String result, String username, String classification) {
        Map<String, Object> fields = ObservabilityLog.event(event);
        fields.put("result", result);
        fields.put("username", username == null || username.isBlank() ? null : username);
        fields.put("method", request.getMethod());
        fields.put("path", request.getRequestURI());
        fields.put("remote_addr", request.getRemoteAddr());
        fields.put("x_forwarded_for_raw", request.getHeader("X-Forwarded-For"));
        fields.put("cf_connecting_ip_raw", request.getHeader("CF-Connecting-IP"));
        fields.put("effective_client_ip", request.getRemoteAddr());
        fields.put("client_ip_trusted", false);
        fields.put("classification", classification);
        ObservabilityLog.write(ObservabilityLog.SECURITY, fields);
    }
}
