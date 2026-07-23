package com.aipm.cowriting.application.dto.guide;

import com.aipm.cowriting.domain.model.GuideTaskStatus;

public record ProjectGuideTaskResponse(
        String id,
        String phase,
        String title,
        String description,
        String reason,
        String expectedOutcome,
        GuideTaskStatus status,
        String targetPath,
        boolean blocking,
        String progressLabel
) {
}
