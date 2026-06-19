package com.aipm.cowriting.application.dto.export;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExportRequest(
        @NotBlank(message = "format 不能为空")
        String format,
        Boolean includeComments,
        @Size(max = 50, message = "citationStyle 长度不能超过 50")
        String citationStyle
) {
}
