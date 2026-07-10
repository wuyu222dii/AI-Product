package com.aipm.cowriting.application.dto.literature;

import java.util.List;

public record LiteratureSearchRequest(
        String query,
        String source,
        Integer limit,
        String missingItemType,
        List<String> providers,
        Integer yearFrom,
        Integer yearTo,
        List<String> workTypes,
        String languageHint,
        String searchIntent
) {
}
