package com.aipm.cowriting.application.dto.material;

import java.util.UUID;

public record MaterialPreviewResponse(
        UUID id,
        String filename,
        String fileType,
        String previewType,
        String previewText,
        String downloadUrl,
        String externalLink
) {
}
