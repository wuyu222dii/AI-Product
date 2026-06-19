package com.aipm.cowriting.application.dto.material;

public record ParseQualityIssue(
        String code,
        String level,
        String label,
        String message,
        String suggestedAction,
        String supplementPrompt
) {
}
