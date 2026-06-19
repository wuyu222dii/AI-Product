package com.aipm.cowriting.application.dto.cowrite;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record CoWritePreviewResponse(
        UUID id,
        UUID workspaceId,
        UUID draftVersionId,
        String action,
        Map<String, Object> targetRange,
        String instruction,
        Map<String, Object> controls,
        String candidateTitleSuggestion,
        String candidateDraftText,
        Map<String, Object> candidateSourceTraceMap,
        Map<String, Object> diffSummary,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime appliedAt
) {
}
