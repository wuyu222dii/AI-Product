package com.aipm.cowriting.application.dto.evidence;

import jakarta.validation.constraints.NotBlank;

public record UpdateEvidenceBindingStatusRequest(
        @NotBlank(message = "status 不能为空")
        String status
) {
}
