package com.aipm.cowriting.application.dto.academic;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateDocumentSectionRequest(
        UUID parentSectionId,
        @Min(0) Integer sortOrder,
        @NotBlank @Size(max = 64) String sectionType,
        @NotBlank @Size(max = 300) String title,
        String content,
        @Min(100) @Max(100000) Integer targetLength
) {
}
