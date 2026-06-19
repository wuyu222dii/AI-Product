package com.aipm.cowriting.application.dto.review;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record AppealResponse(
        UUID id,
        UUID reviewItemId,
        String userReason,
        Map<String, Object> evidence,
        String reviewOutcome,
        OffsetDateTime resolvedAt,
        OffsetDateTime createdAt
) {
}
