package com.smartproperty.platform.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;

/** Shared JWT request metadata for controllers and behavior audit logs. */
public final class AuthenticatedRequest {
    private AuthenticatedRequest() {
    }

    public static Long accountId(Jwt jwt, HttpServletRequest request) {
        String username = jwt.getClaimAsString("username");
        RequestAuditContext.setActor(request,
                username == null || username.isBlank() ? "account:" + jwt.getSubject() : username);
        return Long.valueOf(jwt.getSubject());
    }
}
