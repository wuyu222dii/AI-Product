package com.aipm.cowriting.application.dto.requirement;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record RequirementSnapshotResponse(
        UUID id,
        UUID workspaceId,
        UUID documentId,
        String sourceType,
        String topic,
        Integer wordCount,
        OffsetDateTime deadline,
        String citationStyle,
        Map<String, Object> specialRequirements,
        Integer version,
        OffsetDateTime createdAt
) {
    public RequirementSnapshotResponse(
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
        this(id, workspaceId, null, "PROJECT", topic, wordCount, deadline, citationStyle, specialRequirements, version, createdAt);
    }
}
