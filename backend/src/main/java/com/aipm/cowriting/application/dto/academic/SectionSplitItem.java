package com.aipm.cowriting.application.dto.academic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SectionSplitItem(
        @NotBlank @Size(max = 300) String title,
        @NotBlank @Size(max = 64) String sectionType,
        @NotBlank String content
) {
}
