package com.aipm.cowriting.application.dto.writingrisk;

import java.util.List;
import java.util.UUID;

public record WritingRiskSummaryResponse(
        UUID draftId,
        String overallStatus,
        int overallScore,
        List<WritingRiskItemResponse> items,
        List<String> recommendations
) {
}
