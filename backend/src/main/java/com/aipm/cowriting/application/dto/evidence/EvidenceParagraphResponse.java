package com.aipm.cowriting.application.dto.evidence;

import java.util.List;

public record EvidenceParagraphResponse(
        String paragraphId,
        String paragraphText,
        String bindingStatus,
        List<EvidenceBindingItemResponse> bindings
) {
}
