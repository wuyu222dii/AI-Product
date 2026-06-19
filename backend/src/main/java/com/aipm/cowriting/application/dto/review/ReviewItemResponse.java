package com.aipm.cowriting.application.dto.review;

import com.aipm.cowriting.domain.model.ReviewImpactLevel;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ReviewItemResponse(
        UUID id,
        String reviewType,
        ReviewImpactLevel reviewImpactLevel,
        Map<String, Object> targetRange,
        String message,
        String suggestedFix,
        boolean canBypass,
        String reviewStatus,
        String resolutionNote,
        OffsetDateTime resolvedAt,
        OffsetDateTime lastRecheckedAt,
        String recheckNote,
        List<ReviewRecheckLogResponse> recheckHistory,
        OffsetDateTime createdAt
) {
}
