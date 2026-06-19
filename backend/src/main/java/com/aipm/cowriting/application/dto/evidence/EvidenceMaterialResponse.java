package com.aipm.cowriting.application.dto.evidence;

import java.util.Map;
import java.util.UUID;

public record EvidenceMaterialResponse(
        UUID materialId,
        String filename,
        String fileType,
        String sourceType,
        boolean keyMaterial,
        Map<String, Object> bibliographicMetadata
) {
}
