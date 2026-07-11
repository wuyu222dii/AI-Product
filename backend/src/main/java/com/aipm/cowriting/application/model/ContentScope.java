package com.aipm.cowriting.application.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ContentScope(
        String scopeType,
        UUID workspaceId,
        UUID documentId,
        UUID sectionId,
        UUID draftVersionId,
        Integer revision,
        String title,
        String content,
        Map<String, Object> sourceTraceMap,
        String citationStyle,
        Map<String, Object> requirementContext,
        List<UUID> materialIds
) {
    public boolean isSection() {
        return "SECTION".equals(scopeType);
    }

    public boolean isDocument() {
        return "DOCUMENT".equals(scopeType);
    }

    public boolean isLegacyDraft() {
        return "LEGACY_DRAFT".equals(scopeType);
    }
}
