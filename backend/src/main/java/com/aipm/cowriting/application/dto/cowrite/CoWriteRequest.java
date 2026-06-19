package com.aipm.cowriting.application.dto.cowrite;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

public record CoWriteRequest(
        @NotNull(message = "draftVersionId 不能为空")
        UUID draftVersionId,
        @NotBlank(message = "action 不能为空")
        String action,
        Map<String, Object> targetRange,
        String instruction,
        Map<String, Object> controls
) {
}
