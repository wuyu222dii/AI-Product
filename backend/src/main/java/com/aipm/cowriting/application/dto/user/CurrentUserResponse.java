package com.aipm.cowriting.application.dto.user;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CurrentUserResponse(
        UUID id,
        String email,
        String displayName,
        String avatarUrl,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
