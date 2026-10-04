package com.smartproperty.platform.common.api;

import java.util.List;

public record PagedResponse<T>(
        List<T> items,
        int page,
        int pageSize,
        int total,
        int totalPages
) {
    public static <T> PagedResponse<T> of(List<T> items, int page, int pageSize, int total) {
        int pages = total == 0 ? 0 : (int) Math.ceil((double) total / pageSize);
        return new PagedResponse<>(items, page, pageSize, total, pages);
    }
}
