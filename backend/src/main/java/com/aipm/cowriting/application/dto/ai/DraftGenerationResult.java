package com.aipm.cowriting.application.dto.ai;

import java.util.List;
import java.util.Map;

public record DraftGenerationResult(
        String titleSuggestion,
        Map<String, Object> outline,
        List<Map<String, Object>> paragraphSkeletons,
        String draftText,
        Map<String, Object> sourceTraceMap
) {
}
