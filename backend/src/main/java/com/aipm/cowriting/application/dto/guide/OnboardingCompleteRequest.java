package com.aipm.cowriting.application.dto.guide;

import com.aipm.cowriting.application.dto.workspace.CreateWorkspaceRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record OnboardingCompleteRequest(
        @NotNull @Valid CreateWorkspaceRequest workspace,
        String onboardingVersion
) {
}
