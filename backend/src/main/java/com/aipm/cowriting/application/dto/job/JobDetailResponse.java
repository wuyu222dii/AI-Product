package com.aipm.cowriting.application.dto.job;

import java.util.UUID;

public record JobDetailResponse(
        UUID id,
        UUID workspaceId,
        String jobType,
        String status,
        Integer progress,
        Object inputRef,
        Object outputRef,
        String errorMessage,
        String createdAt,
        String updatedAt
) {
}
