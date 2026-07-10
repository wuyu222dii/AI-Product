package com.aipm.cowriting.application.dto.writingrisk;

import java.util.List;
import java.util.Map;

public record WritingRiskItemResponse(
        String paragraphId,
        Map<String, Object> targetRange,
        String riskType,
        String level,
        String paragraphExcerpt,
        List<String> signals,
        String suggestedAction,
        String supplementPrompt,
        String coWriteInstruction
) {
}
