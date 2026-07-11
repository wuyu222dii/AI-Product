package com.aipm.cowriting.application.dto.academic;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AssembledDocumentResponse(
        UUID documentId,
        String title,
        String content,
        int characterCount,
        List<UUID> sectionIds,
        Map<String, Object> sourceTraceMap
) {
}
