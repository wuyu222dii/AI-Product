package com.aipm.cowriting.application.dto.ai;

public record ReviewRecheckResult(
        String outcome,
        String downgradedImpactLevel,
        String note
) {
}
