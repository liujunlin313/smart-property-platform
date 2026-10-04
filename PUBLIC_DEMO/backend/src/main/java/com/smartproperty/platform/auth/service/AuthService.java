package com.smartproperty.platform.auth.service;

import com.smartproperty.platform.auth.dto.response.AccountView;
import com.smartproperty.platform.auth.dto.response.TokenResponse;
import com.smartproperty.platform.auth.dto.request.LoginRequest;
import com.smartproperty.platform.auth.dto.request.LoginRequest;
import com.smartproperty.platform.auth.mapper.AuthMapper;
import com.smartproperty.platform.auth.model.AccountRow;
import com.smartproperty.platform.auth.model.RefreshSessionRow;
import com.smartproperty.platform.common.exception.BusinessException;
import com.smartproperty.platform.common.logging.SecurityEventLog;
import com.smartproperty.platform.infrastructure.security.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
public class AuthService {
    public static final String REFRESH_COOKIE = "smart_property_refresh";
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecurityProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String dummyHash;

    public AuthService(AuthMapper mapper,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       SecurityProperties properties) {
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.properties = properties;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    @Transactional
    public AuthResult login(LoginRequest request, HttpServletRequest servletRequest) {
        String username = request.username().trim();
        AccountRow account = mapper.findAccountByUsername(username);
        if (account == null) {
            passwordEncoder.matches(request.password(), dummyHash);
            SecurityEventLog.write(servletRequest, "LOGIN_FAILURE", "FAILURE", username, "INVALID_CREDENTIALS");
            throw unauthorized();
        }
        if ("INACTIVE".equals(account.status())) {
            SecurityEventLog.write(servletRequest, "LOGIN_FAILURE", "FAILURE", username, "ACCOUNT_INACTIVE");
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCOUNT_INACTIVE", "账号已停用");
        }
        if (!passwordEncoder.matches(request.password(), account.passwordHash())) {
            int attempts = account.failedLoginAttempts() + 1;
            mapper.recordLoginFailure(account.accountId(), attempts, "ACTIVE", null);
            log.warn("[AUTH FAILURE] username={} reason=invalid_credentials attempts={}", username, attempts);
            SecurityEventLog.write(servletRequest, "LOGIN_FAILURE", "FAILURE", username, "INVALID_CREDENTIALS");
            throw unauthorized();
        }
        mapper.recordLoginSuccess(account.accountId());
        AccountRow refreshed = mapper.findAccountById(account.accountId());
        SecurityEventLog.write(servletRequest, "LOGIN_SUCCESS", "SUCCESS", refreshed.username(), null);
        return issue(refreshed, servletRequest.getHeader("User-Agent"));
    }

    @Transactional
    public AuthResult refresh(String rawRefreshToken, HttpServletRequest servletRequest) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            SecurityEventLog.write(servletRequest, "REFRESH_FAILURE", "FAILURE", null, "MISSING_REFRESH_TOKEN");
            throw unauthorized();
        }
        String oldHash = sha256(rawRefreshToken);
        RefreshSessionRow session = mapper.findRefreshSessionForUpdate(oldHash);
        LocalDateTime now = LocalDateTime.now();
        if (session == null || session.revokedAt() != null || !session.expiresAt().isAfter(now)) {
            SecurityEventLog.write(servletRequest, "REFRESH_FAILURE", "FAILURE", null, "INVALID_OR_EXPIRED_REFRESH");
            throw unauthorized();
        }
        AccountRow account = mapper.findAccountById(session.accountId());
        if (account == null || !"ACTIVE".equals(account.status())) {
            SecurityEventLog.write(servletRequest, "REFRESH_FAILURE", "FAILURE", null, "ACCOUNT_UNAVAILABLE");
            throw unauthorized();
        }
        String replacement = randomRefreshToken();
        String replacementHash = sha256(replacement);
        int rotated = mapper.rotateRefreshSession(session.refreshTokenSessionId(), replacementHash, now);
        if (rotated != 1) {
            mapper.revokeAllRefreshSessions(account.accountId(), now);
            SecurityEventLog.write(servletRequest, "REFRESH_FAILURE", "FAILURE", account.username(), "ROTATION_CONFLICT");
            throw unauthorized();
        }
        LocalDateTime expiresAt = now.plusHours(properties.refreshTokenHours());
        mapper.insertRefreshSession(account.accountId(), replacementHash, expiresAt, servletRequest.getHeader("User-Agent"));
        return tokenResult(account, replacement);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            mapper.revokeRefreshSession(sha256(rawRefreshToken), LocalDateTime.now());
        }
    }

    public AccountView currentAccount(Long accountId) {
        AccountRow account = mapper.findAccountById(accountId);
        if (account == null || !"ACTIVE".equals(account.status())) {
            throw unauthorized();
        }
        return new AccountView(String.valueOf(account.accountId()), account.username(), mapper.findGlobalRoleCodes(account.accountId()));
    }

    public ResponseCookie refreshCookie(String token) {
        return ResponseCookie.from(REFRESH_COOKIE, token)
                .httpOnly(true)
                .secure(properties.refreshCookieSecure())
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(Duration.ofHours(properties.refreshTokenHours()))
                .build();
    }

    public ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(properties.refreshCookieSecure())
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(Duration.ZERO)
                .build();
    }

    private AuthResult issue(AccountRow account, String userAgent) {
        String refreshToken = randomRefreshToken();
        mapper.insertRefreshSession(
                account.accountId(),
                sha256(refreshToken),
                LocalDateTime.now().plusHours(properties.refreshTokenHours()),
                userAgent);
        return tokenResult(account, refreshToken);
    }

    private AuthResult tokenResult(AccountRow account, String refreshToken) {
        List<String> roles = mapper.findGlobalRoleCodes(account.accountId());
        String accessToken = jwtService.createAccessToken(account, roles);
        AccountView view = new AccountView(String.valueOf(account.accountId()), account.username(), roles);
        return new AuthResult(new TokenResponse(accessToken, jwtService.accessTokenSeconds(), view), refreshToken);
    }

    private String randomRefreshToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private BusinessException unauthorized() {
        return new BusinessException(HttpStatus.UNAUTHORIZED, "LOGIN_FAILED", "用户名或密码不正确");
    }

    public record AuthResult(TokenResponse response, String refreshToken) {
    }
}
