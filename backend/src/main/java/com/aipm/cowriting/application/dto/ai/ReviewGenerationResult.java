package com.aipm.cowriting.application.dto.ai;

import java.util.List;
import java.util.Map;

public record ReviewGenerationResult(
        List<Map<String, Object>> items
) {
}
