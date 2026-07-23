package com.aipm.cowriting.application.dto.user;

import com.aipm.cowriting.domain.model.OnboardingStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateOnboardingRequest(
        @NotNull OnboardingStatus status,
        @Size(max = 32) String onboardingVersion
) {
}
