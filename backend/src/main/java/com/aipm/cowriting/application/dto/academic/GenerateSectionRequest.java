package com.aipm.cowriting.application.dto.academic;

import jakarta.validation.constraints.Size;

public record GenerateSectionRequest(
        @Size(max = 4000) String instruction,
        @Size(max = 32) String mode
) {
}
