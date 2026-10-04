package com.smartproperty.platform.common.api;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

/** Standard paged payload carried inside ApiResponse.data. */
public record PageResult<T>(
        List<T> records,
        @JsonSerialize(using = PaginationLongNumberSerializer.class)
        long total,
        int page,
        int pageSize
) {
    public static <T> PageResult<T> of(List<T> records, long total, int page, int pageSize) {
        return new PageResult<>(records, total, page, pageSize);
    }
}
