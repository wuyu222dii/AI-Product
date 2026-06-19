package com.aipm.cowriting.application.dto.requirement;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record RequirementSnapshotResponse(
        UUID id,
        UUID workspaceId,
        String topic,
        Integer wordCount,
        OffsetDateTime deadline,
        String citationStyle,
        Map<String, Object> specialRequirements,
        Integer version,
        OffsetDateTime createdAt
) {
}
