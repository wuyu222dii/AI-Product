package com.aipm.cowriting.application.dto.knowledge;

import jakarta.validation.constraints.NotBlank;

public record KnowledgeSearchRequest(
        @NotBlank(message = "请输入要检索的问题或关键词")
        String query,
        Integer limit
) {
}
