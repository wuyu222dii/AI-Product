package com.aipm.cowriting.application.dto.sufficiency;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record MaterialSufficiencyResponse(
        UUID id,
        UUID workspaceId,
        boolean isGenerationEligible,
        List<Map<String, Object>> missingItems,
        List<Map<String, Object>> recommendedSupplements,
        String reason
) {
}
