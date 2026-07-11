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
        OffsetDateTime createdAt,
        String scopeType,
        UUID documentId,
        UUID sectionId,
        Integer sectionVersionNo,
        String issueFingerprint,
        String analysisState
) {
    public ReviewItemResponse(
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
        this(id, reviewType, reviewImpactLevel, targetRange, message, suggestedFix, canBypass,
                reviewStatus, resolutionNote, resolvedAt, lastRecheckedAt, recheckNote, recheckHistory,
                createdAt, "LEGACY_DRAFT", null, null, null, null, "CURRENT");
    }
}
