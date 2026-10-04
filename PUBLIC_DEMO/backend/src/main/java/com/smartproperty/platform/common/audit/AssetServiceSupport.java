package com.smartproperty.platform.common.audit;

import com.smartproperty.platform.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public final class AssetServiceSupport {
    private AssetServiceSupport() {
    }

    public static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static BusinessException notFound(String code, String message) {
        return new BusinessException(HttpStatus.NOT_FOUND, code, message);
    }

    public static BusinessException conflict() {
        return new BusinessException(HttpStatus.CONFLICT, "OPTIMISTIC_LOCK_CONFLICT",
                "数据已被其他操作修改，请刷新后重试");
    }

    public static BusinessException invalid(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_ASSET_DATA", message);
    }
}
