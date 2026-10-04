package com.smartproperty.platform.auth.dto.response;

public record TokenResponse(String accessToken, long expiresInSeconds, AccountView account) {
}
