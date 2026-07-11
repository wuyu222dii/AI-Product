package com.aipm.cowriting.application.dto.academic;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentMaterialLinkResponse(
        UUID id,
        UUID documentId,
        UUID materialId,
        String materialName,
        String role,
        boolean included,
        OffsetDateTime updatedAt
) {
}
