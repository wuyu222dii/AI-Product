package com.aipm.cowriting.application.dto.literature;

import java.util.List;
import java.util.Map;

public record LiteratureSearchResponse(
        String query,
        List<LiteratureSearchItem> items,
        List<ExternalSearchLink> externalSearchLinks,
        Map<String, String> providerStatus
) {
}
