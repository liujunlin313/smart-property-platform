package com.smartproperty.platform.infrastructure.observability;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.InetAddress;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import com.smartproperty.platform.common.exception.BusinessException;

/**
 * Phase 2A Stabilization - 2026-08-22
 * Receives bounded local browser action/error batches so IDEA and PowerShell show
 * the route and last semantic action that preceded a failure.
 */
@RestController
@RequestMapping("/api/v1/dev/client-events")
@ConditionalOnProperty(name = "app.dev.client-events-enabled", havingValue = "true")
public class ClientEventController {
    private static final Logger consoleLog = LoggerFactory.getLogger(ClientEventController.class);
    private static final Logger detailLog = LoggerFactory.getLogger("FRONTEND_CLIENT");
    private static final String CYAN = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";
    private static final String RESET = "\u001B[0m";
    private static final Pattern SECRET = Pattern.compile(
            "(?i)(bearer\\s+[a-z0-9._~+/-]+|password\\s*[:=]\\s*[^,; ]+|cookie\\s*[:=]\\s*[^,;]+|token\\s*[:=]\\s*[^,; ]+)");
    private static final ConcurrentHashMap<String, RequestWindow> WINDOWS = new ConcurrentHashMap<>();

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void receive(@Valid @RequestBody ClientEventBatch batch, HttpServletRequest request) {
        if (!isLoopback(request.getRemoteAddr())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (!WINDOWS.computeIfAbsent(request.getRemoteAddr(), ignored -> new RequestWindow()).allow()) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS,
                    "CLIENT_EVENT_RATE_LIMIT", "浏览器诊断事件提交过于频繁");
        }
        for (ClientEventBatch.ClientEvent event : batch.events()) {
            logEvent(event);
        }
    }

    private void logEvent(ClientEventBatch.ClientEvent event) {
        String type = clean(event.type(), 24).toUpperCase(Locale.ROOT);
        String route = clean(event.route(), 240);
        String action = clean(event.action(), 100);
        if ("ACTION".equals(type) || "ROUTE".equals(type) || "SUBMIT".equals(type)) {
            consoleLog.info("{}[UI ACTION]{} route={} action={}", CYAN, RESET, route, action);
            detailLog.info("type={} route={} action={}", type, route, action);
            return;
        }
        if ("HTTP_ERROR".equals(type)) {
            consoleLog.error("{}[HTTP ERROR]{} route={} request={} {} status={} message={}",
                    RED, RESET, route, clean(event.method(), 12), clean(event.url(), 240),
                    event.httpStatus(), clean(event.message(), 600));
            detailLog.error("type={} route={} action={} request={} {} status={} message={} stack={}",
                    type, route, action, clean(event.method(), 12), clean(event.url(), 240),
                    event.httpStatus(), clean(event.message(), 600), clean(event.stack(), 2_000));
            return;
        }
        if ("OFFLINE".equals(type) || "WARN".equals(type)) {
            consoleLog.warn("{}[FRONTEND WARN]{} route={} action={} message={}",
                    YELLOW, RESET, route, action, clean(event.message(), 600));
            detailLog.warn("type={} route={} action={} message={}",
                    type, route, action, clean(event.message(), 600));
            return;
        }
        consoleLog.error("{}[FRONTEND ERROR]{} type={} route={} action={} message={}",
                RED, RESET, type, route, action, clean(event.message(), 600));
        detailLog.error("type={} route={} action={} message={} stack={}",
                type, route, action, clean(event.message(), 600), clean(event.stack(), 2_000));
    }

    private String clean(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String oneLine = value.replace('\r', ' ').replace('\n', ' ')
                .replace("\u001B", "").trim();
        oneLine = SECRET.matcher(oneLine).replaceAll("[REDACTED]");
        return oneLine.substring(0, Math.min(oneLine.length(), maxLength));
    }

    private boolean isLoopback(String remoteAddress) {
        try {
            return InetAddress.getByName(remoteAddress).isLoopbackAddress();
        } catch (Exception exception) {
            return false;
        }
    }

    private static final class RequestWindow {
        private long startedAt = System.currentTimeMillis();
        private int requests;

        synchronized boolean allow() {
            long now = System.currentTimeMillis();
            if (now - startedAt >= 10_000L) {
                startedAt = now;
                requests = 0;
            }
            return ++requests <= 30;
        }
    }
}
