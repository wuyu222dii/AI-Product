package com.aipm.cowriting.application.dto.review;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record ReviewRecheckLogResponse(
        UUID id,
        String outcome,
        String previousStatus,
        String nextStatus,
        String previousImpactLevel,
        String nextImpactLevel,
        String note,
        Map<String, Object> basis,
        OffsetDateTime createdAt
) {
}
