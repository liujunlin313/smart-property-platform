package com.smartproperty.platform.common.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HttpAccessLog {
    public static final Logger CONSOLE = LoggerFactory.getLogger("HTTP_CONSOLE");
    public static final Logger FILE = LoggerFactory.getLogger("HTTP_ACCESS");

    private HttpAccessLog() {
    }
}
