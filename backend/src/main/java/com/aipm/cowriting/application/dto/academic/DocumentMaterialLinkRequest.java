package com.aipm.cowriting.application.dto.academic;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record DocumentMaterialLinkRequest(
        @NotNull UUID materialId,
        @Size(max = 64) String role,
        Boolean included
) {
}
