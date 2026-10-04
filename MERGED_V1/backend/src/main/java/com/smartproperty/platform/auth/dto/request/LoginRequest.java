package com.smartproperty.platform.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "请输入用户名") @Size(max = 64) String username,
        @NotBlank(message = "请输入密码") @Size(max = 128) String password
) {
}
