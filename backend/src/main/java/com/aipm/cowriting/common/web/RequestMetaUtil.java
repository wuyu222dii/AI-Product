package com.aipm.cowriting.common.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;

public final class RequestMetaUtil {

    private RequestMetaUtil() {
    }

    public static Map<String, Object> meta(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        return Map.of("requestId", requestId);
    }
}
