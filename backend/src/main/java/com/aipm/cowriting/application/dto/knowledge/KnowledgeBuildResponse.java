package com.aipm.cowriting.application.dto.knowledge;

import java.util.UUID;

public record KnowledgeBuildResponse(
        UUID workspaceId,
        int materialCount,
        int chunkCount,
        String status,
        String message
) {
}
