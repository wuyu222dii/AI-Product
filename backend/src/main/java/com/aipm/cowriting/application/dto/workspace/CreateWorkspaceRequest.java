package com.aipm.cowriting.application.dto.workspace;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWorkspaceRequest(
        @NotBlank(message = "title 不能为空")
        @Size(max = 120, message = "title 长度不能超过 120")
        String title
) {
}
