package com.aipm.cowriting.application.dto.review;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record AppealRequest(
        @NotBlank(message = "userReason 不能为空")
        @Size(max = 2000, message = "userReason 长度不能超过 2000")
        String userReason,
        Map<String, Object> evidence
) {
}
