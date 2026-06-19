package com.aipm.cowriting.application.dto.workspace;

import com.aipm.cowriting.domain.model.WorkspaceStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record WorkspaceResponse(
        UUID id,
        String title,
        WorkspaceStatus status,
        UUID currentDraftVersionId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
