package com.aipm.cowriting.application.dto.draft;

import com.aipm.cowriting.domain.model.GenerationStatus;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record DraftResponse(
        UUID id,
        UUID workspaceId,
        Integer versionNo,
        String titleSuggestion,
        Map<String, Object> outline,
        Object paragraphSkeletons,
        String draftText,
        Map<String, Object> sourceTraceMap,
        GenerationStatus generationStatus,
        String createdBy,
        OffsetDateTime createdAt
) {
}
