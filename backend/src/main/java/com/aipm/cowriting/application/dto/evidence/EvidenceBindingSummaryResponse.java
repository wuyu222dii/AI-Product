package com.aipm.cowriting.application.dto.evidence;

import java.util.List;
import java.util.UUID;

public record EvidenceBindingSummaryResponse(
        UUID draftVersionId,
        List<EvidenceParagraphResponse> paragraphs,
        List<String> missingParagraphIds,
        List<EvidenceMaterialResponse> usedMaterials,
        List<EvidenceMaterialResponse> unusedMaterials
) {
}
