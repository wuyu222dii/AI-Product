package com.aipm.cowriting.application.dto.academic;

import com.aipm.cowriting.domain.model.DocumentSectionStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record UpdateDocumentSectionRequest(
        @Size(max = 300) String title,
        String content,
        @Min(0) Integer sortOrder,
        @Min(100) @Max(100000) Integer targetLength,
        DocumentSectionStatus status,
        Map<String, Object> sourceTraceMap,
        @Size(max = 500) String changeSummary
) {
}
