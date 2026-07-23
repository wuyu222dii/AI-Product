package com.aipm.cowriting.application.dto.user;

import com.aipm.cowriting.domain.model.OnboardingStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CurrentUserResponse(
        UUID id,
        String email,
        String displayName,
        String avatarUrl,
        OnboardingStatus onboardingStatus,
        String onboardingVersion,
        OffsetDateTime onboardingCompletedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
