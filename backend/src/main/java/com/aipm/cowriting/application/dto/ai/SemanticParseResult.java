package com.aipm.cowriting.application.dto.ai;

import com.aipm.cowriting.application.dto.reference.BibliographicMetadata;
import java.math.BigDecimal;
import java.util.List;

public record SemanticParseResult(
        String materialCategory,
        String summary,
        String topicRelation,
        List<String> detectedClaims,
        List<String> detectedEvidence,
        List<String> detectedRequirements,
        BibliographicMetadata bibliographicMetadata,
        BigDecimal confidenceScore
) {
}
