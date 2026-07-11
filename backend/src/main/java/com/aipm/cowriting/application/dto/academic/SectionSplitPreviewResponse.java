package com.aipm.cowriting.application.dto.academic;

import java.util.List;
import java.util.UUID;

public record SectionSplitPreviewResponse(
        UUID sectionId,
        Integer baseVersionNo,
        boolean canApply,
        List<SectionSplitItem> sections,
        String message
) {
}
