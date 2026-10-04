package com.smartproperty.platform.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.Map;

/** Stable JSON envelope for every business API response. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        Map<String, String> fieldErrors,
        Integer status,
        String traceId,
        OffsetDateTime timestamp
) {
    public static <T> ApiResponse<T> success(T data, int status, String traceId) {
        return new ApiResponse<>(true, "SUCCESS", status == 201 ? "创建成功" : "操作成功",
                data, null, status, traceId, OffsetDateTime.now());
    }

    public static <T> ApiResponse<T> failure(String code,
                                             String message,
                                             Map<String, String> fieldErrors,
                                             int status,
                                             String traceId) {
        return new ApiResponse<>(false, code, message, null, fieldErrors,
                status, traceId, OffsetDateTime.now());
    }
}
