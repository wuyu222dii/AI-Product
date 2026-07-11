package com.aipm.cowriting.application.dto.academic;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AiActionLogResponse(
        UUID id,
        UUID workspaceId,
        UUID documentId,
        UUID sectionId,
        String actionType,
        String modelName,
        List<UUID> evidenceMaterialIds,
        String requestSummary,
        String outputSummary,
        Boolean accepted,
        boolean disclosureRequired,
        OffsetDateTime createdAt
) {
}
