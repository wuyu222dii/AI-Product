package com.aipm.cowriting.common.api;

public record Pagination(
        int page,
        int pageSize,
        long total
) {
}
