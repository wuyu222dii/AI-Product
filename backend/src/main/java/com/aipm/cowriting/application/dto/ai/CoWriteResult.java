package com.aipm.cowriting.application.dto.ai;

import java.util.Map;

public record CoWriteResult(
        String titleSuggestion,
        String draftText,
        Map<String, Object> sourceTraceMap
) {
}
