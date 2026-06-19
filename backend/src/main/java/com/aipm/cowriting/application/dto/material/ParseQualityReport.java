package com.aipm.cowriting.application.dto.material;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ParseQualityReport(
        String status,
        BigDecimal score,
        List<ParseQualityIssue> issues,
        Map<String, Boolean> completeness,
        String nextAction
) {
}
