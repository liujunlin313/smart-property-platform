package com.smartproperty.platform.auth.controller;

import com.smartproperty.platform.auth.dto.request.LoginRequest;
import com.smartproperty.platform.auth.dto.response.AccountView;
import com.smartproperty.platform.auth.dto.response.TokenResponse;
import com.smartproperty.platform.auth.service.AuthService;

import com.smartproperty.platform.common.web.RequestAuditContext;
import com.smartproperty.platform.common.logging.SecurityEventLog;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest servletRequest) {
        AuthService.AuthResult result = service.login(request, servletRequest);
        RequestAuditContext.setActor(servletRequest, result.response().account().username());
        return withRefreshCookie(result);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = AuthService.REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletRequest servletRequest) {
        AuthService.AuthResult result = service.refresh(refreshToken, servletRequest);
        RequestAuditContext.setActor(servletRequest, result.response().account().username());
        return withRefreshCookie(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Boolean>> logout(
            @CookieValue(name = AuthService.REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletRequest servletRequest) {
        service.logout(refreshToken);
        SecurityEventLog.write(servletRequest, "LOGOUT", "SUCCESS", RequestAuditContext.actor(servletRequest), null);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, service.clearRefreshCookie().toString())
                .body(Map.of("loggedOut", true));
    }

    @GetMapping("/me")
    public AccountView me(@org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt) {
        return service.currentAccount(Long.valueOf(jwt.getSubject()));
    }

    private ResponseEntity<TokenResponse> withRefreshCookie(AuthService.AuthResult result) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, service.refreshCookie(result.refreshToken()).toString())
                .body(result.response());
    }
}
