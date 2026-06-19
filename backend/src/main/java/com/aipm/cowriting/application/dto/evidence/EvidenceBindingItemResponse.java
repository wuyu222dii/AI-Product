package com.aipm.cowriting.application.dto.evidence;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record EvidenceBindingItemResponse(
        UUID id,
        UUID draftVersionId,
        String paragraphId,
        UUID materialId,
        UUID knowledgeChunkId,
        String materialTitle,
        String claimText,
        String sourceExcerpt,
        Map<String, Object> sourceLocation,
        Map<String, Object> targetRange,
        BigDecimal confidenceScore,
        String supportType,
        String bindingStatus,
        String citationText,
        Map<String, Object> bibliographicMetadata,
        OffsetDateTime createdAt
) {
}
