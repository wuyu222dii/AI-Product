package com.aipm.cowriting.application.dto.evidence;

import java.util.List;

public record EvidenceCoverageReport(
        int totalParagraphs,
        int confirmedParagraphs,
        int weakParagraphs,
        int missingParagraphs,
        int coverageRatio,
        int confirmedRatio,
        String healthLabel,
        List<String> recommendations
) {
}
