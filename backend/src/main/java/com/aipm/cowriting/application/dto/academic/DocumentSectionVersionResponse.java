package com.aipm.cowriting.application.dto.academic;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record DocumentSectionVersionResponse(
        UUID id,
        UUID sectionId,
        Integer versionNo,
        String title,
        String content,
        Map<String, Object> sourceTraceMap,
        String changeSource,
        String changeSummary,
        OffsetDateTime createdAt
) {
}
