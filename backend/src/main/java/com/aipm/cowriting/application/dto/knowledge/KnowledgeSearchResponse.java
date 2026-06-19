package com.aipm.cowriting.application.dto.knowledge;

import java.util.List;
import java.util.UUID;

public record KnowledgeSearchResponse(
        UUID workspaceId,
        String query,
        int total,
        List<KnowledgeChunkResponse> items
) {
}
