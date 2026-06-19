package com.aipm.cowriting.application.dto.knowledge;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record KnowledgeChunkResponse(
        UUID id,
        UUID workspaceId,
        UUID materialId,
        String materialTitle,
        int chunkIndex,
        String chunkText,
        String sourceExcerpt,
        List<String> keywords,
        double score,
        OffsetDateTime createdAt
) {
}
