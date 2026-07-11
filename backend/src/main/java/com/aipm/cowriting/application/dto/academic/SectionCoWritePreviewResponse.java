package com.aipm.cowriting.application.dto.academic;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SectionCoWritePreviewResponse(
        UUID id,
        UUID sectionId,
        Integer baseVersionNo,
        String action,
        String instruction,
        Map<String, Object> controls,
        String candidateContent,
        Map<String, Object> candidateSourceTraceMap,
        Map<String, Object> diffSummary,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime appliedAt,
        String baseContent,
        Map<String, Object> targetRange,
        List<Map<String, Object>> diffRows,
        List<Map<String, Object>> paragraphDiffRows,
        List<UUID> relatedReviewItemIds
) {
    public SectionCoWritePreviewResponse(
            UUID id,
            UUID sectionId,
            Integer baseVersionNo,
            String action,
            String instruction,
            Map<String, Object> controls,
            String candidateContent,
            Map<String, Object> candidateSourceTraceMap,
            Map<String, Object> diffSummary,
            String status,
            OffsetDateTime createdAt,
            OffsetDateTime appliedAt
    ) {
        this(id, sectionId, baseVersionNo, action, instruction, controls, candidateContent,
                candidateSourceTraceMap, diffSummary, status, createdAt, appliedAt, "", Map.of(),
                List.of(), List.of(), List.of());
    }
}
