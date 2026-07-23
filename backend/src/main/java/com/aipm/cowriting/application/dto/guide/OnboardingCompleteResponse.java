package com.aipm.cowriting.application.dto.guide;

import com.aipm.cowriting.application.dto.user.CurrentUserResponse;
import com.aipm.cowriting.application.dto.workspace.WorkspaceResponse;

public record OnboardingCompleteResponse(
        CurrentUserResponse user,
        WorkspaceResponse workspace,
        ProjectGuideResponse guide
) {
}
