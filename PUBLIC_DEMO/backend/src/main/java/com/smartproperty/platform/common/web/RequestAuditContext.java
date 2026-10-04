package com.smartproperty.platform.common.web;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestAuditContext {
    private static final String ACTOR_ATTRIBUTE = RequestAuditContext.class.getName() + ".actor";

    private RequestAuditContext() {
    }

    public static void setActor(HttpServletRequest request, String actor) {
        if (actor != null && !actor.isBlank()) {
            request.setAttribute(ACTOR_ATTRIBUTE, actor);
        }
    }

    public static String actor(HttpServletRequest request) {
        Object value = request.getAttribute(ACTOR_ATTRIBUTE);
        return value == null ? null : value.toString();
    }
}
