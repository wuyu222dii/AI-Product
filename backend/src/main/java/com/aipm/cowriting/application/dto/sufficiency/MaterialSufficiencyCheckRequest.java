package com.aipm.cowriting.application.dto.sufficiency;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MaterialSufficiencyCheckRequest(
        @NotNull(message = "requirementSnapshotId 不能为空")
        UUID requirementSnapshotId
) {
}
