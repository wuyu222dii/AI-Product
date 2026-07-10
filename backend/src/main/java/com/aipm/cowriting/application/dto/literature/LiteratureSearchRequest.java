package com.aipm.cowriting.application.dto.literature;

public record LiteratureSearchRequest(
        String query,
        String source,
        Integer limit,
        String missingItemType
) {
}
