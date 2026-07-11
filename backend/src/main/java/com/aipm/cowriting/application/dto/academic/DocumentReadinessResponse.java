package com.aipm.cowriting.application.dto.academic;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DocumentReadinessResponse(
        UUID documentId,
        String status,
        int score,
        boolean generationEligible,
        List<ReadinessIssueResponse> issues,
        Map<String, Boolean> artifactCoverage,
        String nextAction
) {
}
