package com.aipm.cowriting.application.dto.academic;

import com.aipm.cowriting.domain.model.DocumentSectionStatus;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record DocumentSectionResponse(
        UUID id,
        UUID documentId,
        UUID parentSectionId,
        Integer sortOrder,
        String sectionType,
        String title,
        String content,
        Integer targetLength,
        DocumentSectionStatus status,
        Map<String, Object> sourceTraceMap,
        Integer versionNo,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
