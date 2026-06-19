package com.aipm.cowriting.application.dto.draft;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record GenerateDraftRequest(
        @NotNull(message = "requirementSnapshotId 不能为空")
        UUID requirementSnapshotId,
        String mode
) {
}
