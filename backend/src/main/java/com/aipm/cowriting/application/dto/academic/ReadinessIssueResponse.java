package com.aipm.cowriting.application.dto.academic;

import java.util.UUID;

public record ReadinessIssueResponse(
        String code,
        String level,
        String label,
        String message,
        String suggestedAction,
        UUID sectionId
) {
}
