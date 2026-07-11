package com.aipm.cowriting.application.dto.evidence;

import java.util.List;
import java.util.UUID;

public record EvidenceBindingSummaryResponse(
        UUID draftVersionId,
        List<EvidenceParagraphResponse> paragraphs,
        List<String> missingParagraphIds,
        List<EvidenceMaterialResponse> usedMaterials,
        List<EvidenceMaterialResponse> unusedMaterials,
        EvidenceCoverageReport coverage,
        CitationConsistencyReport citationConsistency,
        String scopeType,
        UUID documentId,
        UUID sectionId,
        Integer sectionVersionNo,
        String analysisState
) {
    public EvidenceBindingSummaryResponse(
            UUID draftVersionId,
            List<EvidenceParagraphResponse> paragraphs,
            List<String> missingParagraphIds,
            List<EvidenceMaterialResponse> usedMaterials,
            List<EvidenceMaterialResponse> unusedMaterials,
            EvidenceCoverageReport coverage,
            CitationConsistencyReport citationConsistency
    ) {
        this(draftVersionId, paragraphs, missingParagraphIds, usedMaterials, unusedMaterials, coverage,
                citationConsistency, "LEGACY_DRAFT", null, null, null, "CURRENT");
    }
}
