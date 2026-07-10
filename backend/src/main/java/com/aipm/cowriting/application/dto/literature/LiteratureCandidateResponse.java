package com.aipm.cowriting.application.dto.literature;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record LiteratureCandidateResponse(
        UUID id,
        UUID workspaceId,
        String provider,
        String title,
        List<String> authors,
        String year,
        String sourceTitle,
        String publisher,
        String doi,
        String url,
        String abstractSnippet,
        String citationPreview,
        Integer qualityScore,
        String qualityLabel,
        List<String> matchedReasons,
        List<String> missingMetadata,
        String duplicateGroupKey,
        String recommendedUse,
        String status,
        UUID materialId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
