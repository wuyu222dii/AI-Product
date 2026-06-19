package com.aipm.cowriting.application.dto.review;

import jakarta.validation.constraints.NotBlank;

public record UpdateReviewStatusRequest(
        @NotBlank(message = "status 不能为空")
        String status,
        String resolutionNote
) {
}
