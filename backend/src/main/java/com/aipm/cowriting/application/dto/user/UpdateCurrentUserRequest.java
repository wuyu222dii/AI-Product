package com.aipm.cowriting.application.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCurrentUserRequest(
        @NotBlank(message = "displayName 不能为空")
        @Size(max = 80, message = "displayName 长度不能超过 80")
        String displayName
) {
}
