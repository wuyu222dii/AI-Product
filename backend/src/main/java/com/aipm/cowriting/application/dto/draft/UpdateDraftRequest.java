package com.aipm.cowriting.application.dto.draft;

import jakarta.validation.constraints.NotBlank;

public record UpdateDraftRequest(
        String titleSuggestion,
        @NotBlank(message = "draftText 不能为空")
        String draftText
) {
}
