package com.aipm.cowriting.application.dto.knowledge;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

public record KnowledgeSearchRequest(
        @NotBlank(message = "请输入要检索的问题或关键词")
        String query,
        Integer limit,
        UUID documentId,
        List<UUID> materialIds,
        List<String> tags
) {
    public KnowledgeSearchRequest(String query, Integer limit) {
        this(query, limit, null, List.of(), List.of());
    }
}
