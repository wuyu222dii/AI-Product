package com.aipm.cowriting.application.dto.academic;

import com.aipm.cowriting.application.dto.evidence.DocumentEvidenceSummaryResponse;
import com.aipm.cowriting.application.dto.review.ReviewItemResponse;
import com.aipm.cowriting.application.dto.writingrisk.WritingRiskSummaryResponse;
import java.util.List;
import java.util.UUID;

public record DocumentQualitySummaryResponse(
        UUID documentId,
        String status,
        int score,
        DocumentEvidenceSummaryResponse evidence,
        WritingRiskSummaryResponse writingRisks,
        List<ReviewItemResponse> reviewItems,
        List<SectionQualitySummaryResponse> sections,
        List<String> recommendations
) {
}
