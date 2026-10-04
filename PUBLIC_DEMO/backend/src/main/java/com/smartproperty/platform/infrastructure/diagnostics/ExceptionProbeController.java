package com.smartproperty.platform.infrastructure.diagnostics;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Disabled by default; enabled only during local stabilization to verify real 500 logging. */
@RestController
@RequestMapping("/api/v1/dev/exception-probe")
@ConditionalOnProperty(name = "app.dev.exception-probe-enabled", havingValue = "true")
public class ExceptionProbeController {
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void fail() {
        throw new IllegalStateException("ESV2 intentional exception logging probe");
    }
}
