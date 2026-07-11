package com.aipm.cowriting.application.dto.evidence;

import java.util.List;
import java.util.UUID;

public record DocumentEvidenceSummaryResponse(
        UUID documentId,
        List<EvidenceBindingSummaryResponse> sections,
        EvidenceCoverageReport coverage,
        CitationConsistencyReport citationConsistency,
        String analysisState
) {
}
