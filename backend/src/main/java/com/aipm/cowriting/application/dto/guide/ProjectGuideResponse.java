package com.aipm.cowriting.application.dto.guide;

import com.aipm.cowriting.domain.model.GuideMode;
import com.aipm.cowriting.domain.model.GuideProgress;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ProjectGuideResponse(
        UUID workspaceId,
        String guideVersion,
        GuideProgress currentProgress,
        List<String> availableMaterials,
        LocalDate targetDeadline,
        GuideMode preferredMode,
        int overallProgress,
        String currentTaskId,
        List<ProjectGuideTaskResponse> tasks,
        OffsetDateTime updatedAt
) {
}
