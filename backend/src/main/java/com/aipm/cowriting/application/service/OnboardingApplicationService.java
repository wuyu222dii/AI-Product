package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.guide.OnboardingCompleteRequest;
import com.aipm.cowriting.application.dto.guide.OnboardingCompleteResponse;
import com.aipm.cowriting.application.dto.guide.ProjectGuideResponse;
import com.aipm.cowriting.application.dto.user.CurrentUserResponse;
import com.aipm.cowriting.application.dto.workspace.WorkspaceResponse;
import com.aipm.cowriting.domain.entity.UserProfileEntity;
import com.aipm.cowriting.domain.entity.WorkspaceEntity;
import com.aipm.cowriting.domain.model.OnboardingStatus;
import com.aipm.cowriting.domain.repository.WorkspaceRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingApplicationService {

    private final UserProfileApplicationService userProfileService;
    private final WorkspaceApplicationService workspaceService;
    private final ProjectGuideApplicationService guideService;
    private final WorkspaceRepository workspaceRepository;
    private final CurrentUserService currentUserService;

    public OnboardingApplicationService(
            UserProfileApplicationService userProfileService,
            WorkspaceApplicationService workspaceService,
            ProjectGuideApplicationService guideService,
            WorkspaceRepository workspaceRepository,
            CurrentUserService currentUserService
    ) {
        this.userProfileService = userProfileService;
        this.workspaceService = workspaceService;
        this.guideService = guideService;
        this.workspaceRepository = workspaceRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public OnboardingCompleteResponse complete(OnboardingCompleteRequest request) {
        UserProfileEntity profile = userProfileService.lockCurrentProfile();
        Optional<WorkspaceEntity> existing = workspaceRepository
                .findFirstByUserIdOrderByCreatedAtAsc(currentUserService.userId());

        if (existing.isPresent()) {
            WorkspaceResponse workspace = workspaceService.get(existing.get().getId());
            CurrentUserResponse user = profile.getOnboardingStatus() == OnboardingStatus.COMPLETED
                    ? userProfileService.getCurrent()
                    : userProfileService.completeOnboarding(profile, request.onboardingVersion());
            return new OnboardingCompleteResponse(
                    user, workspace, guideService.get(workspace.id())
            );
        }

        WorkspaceResponse workspace = workspaceService.create(request.workspace());
        CurrentUserResponse user = userProfileService.completeOnboarding(profile, request.onboardingVersion());
        ProjectGuideResponse guide = guideService.get(workspace.id());
        return new OnboardingCompleteResponse(user, workspace, guide);
    }
}
