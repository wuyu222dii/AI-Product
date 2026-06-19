package com.aipm.cowriting.common.api;

import java.util.List;

public record PagedResponse<T>(
        List<T> items,
        Pagination pagination
) {
}
