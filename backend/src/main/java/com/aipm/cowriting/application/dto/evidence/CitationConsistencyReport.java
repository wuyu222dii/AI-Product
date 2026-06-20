package com.aipm.cowriting.application.dto.evidence;

import java.util.List;

public record CitationConsistencyReport(
        String status,
        int detectedCitationCount,
        int linkedMaterialCount,
        int missingCitationParagraphCount,
        int orphanCitationCount,
        int incompleteReferenceCount,
        List<String> issues
) {
}
