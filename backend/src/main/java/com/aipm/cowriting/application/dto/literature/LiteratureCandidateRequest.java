package com.aipm.cowriting.application.dto.literature;

import java.util.List;

public record LiteratureCandidateRequest(
        String provider,
        String title,
        List<String> authors,
        String year,
        String sourceTitle,
        String publisher,
        String doi,
        String url,
        String abstractSnippet,
        String citationPreview,
        Integer qualityScore,
        String qualityLabel,
        List<String> matchedReasons,
        List<String> missingMetadata,
        String duplicateGroupKey,
        String recommendedUse
) {
}
