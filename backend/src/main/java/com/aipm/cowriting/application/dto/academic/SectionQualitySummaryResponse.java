package com.aipm.cowriting.application.dto.academic;

import java.util.UUID;

public record SectionQualitySummaryResponse(
        UUID sectionId,
        String title,
        Integer versionNo,
        int evidenceCoverage,
        int writingRiskScore,
        int openReviewCount,
        String analysisState
) {
}
