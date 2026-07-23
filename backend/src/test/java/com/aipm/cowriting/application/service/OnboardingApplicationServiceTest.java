package com.aipm.cowriting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aipm.cowriting.application.dto.guide.OnboardingCompleteRequest;
import com.aipm.cowriting.application.dto.guide.OnboardingCompleteResponse;
import com.aipm.cowriting.application.dto.user.CurrentUserResponse;
import com.aipm.cowriting.application.dto.workspace.CreateWorkspaceRequest;
import com.aipm.cowriting.application.dto.workspace.WorkspaceResponse;
import com.aipm.cowriting.domain.entity.UserProfileEntity;
import com.aipm.cowriting.domain.entity.WorkspaceEntity;
import com.aipm.cowriting.domain.model.OnboardingStatus;
import com.aipm.cowriting.domain.repository.WorkspaceRepository;
import java.util.Optional;
import java.util.UUID;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OnboardingApplicationServiceTest {

    @Mock private UserProfileApplicationService userProfileService;
    @Mock private WorkspaceApplicationService workspaceService;
    @Mock private ProjectGuideApplicationService guideService;
    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private CurrentUserService currentUserService;

    private OnboardingApplicationService service;
    private UUID userId;
    private UserProfileEntity profile;

    @BeforeEach
    void setUp() {
        service = new OnboardingApplicationService(
                userProfileService, workspaceService, guideService, workspaceRepository, currentUserService
        );
        userId = UUID.randomUUID();
        profile = new UserProfileEntity();
        profile.setId(userId);
        profile.setOnboardingStatus(OnboardingStatus.NOT_STARTED);
        when(userProfileService.lockCurrentProfile()).thenReturn(profile);
        when(currentUserService.userId()).thenReturn(userId);
    }

    @Test
    void completeShouldCreateOneWorkspaceAndFinishOnboarding() {
        UUID workspaceId = UUID.randomUUID();
        WorkspaceResponse workspace = workspace(workspaceId);
        OffsetDateTime now = OffsetDateTime.now();
        CurrentUserResponse user = new CurrentUserResponse(
                userId, "user@example.com", "研究者", null,
                OnboardingStatus.COMPLETED, "v1", now, now, now
        );
        when(workspaceRepository.findFirstByUserIdOrderByCreatedAtAsc(userId)).thenReturn(Optional.empty());
        when(workspaceService.create(any())).thenReturn(workspace);
        when(userProfileService.completeOnboarding(profile, "v1")).thenReturn(user);

        OnboardingCompleteResponse response = service.complete(request());

        assertThat(response.workspace().id()).isEqualTo(workspaceId);
        verify(workspaceService).create(any(CreateWorkspaceRequest.class));
        verify(userProfileService).completeOnboarding(profile, "v1");
        verify(guideService).get(workspaceId);
    }

    @Test
    void repeatedCompletionShouldReturnExistingWorkspaceWithoutCreatingAnother() {
        UUID workspaceId = UUID.randomUUID();
        profile.setOnboardingStatus(OnboardingStatus.COMPLETED);
        WorkspaceEntity existing = new WorkspaceEntity();
        existing.setId(workspaceId);
        when(workspaceRepository.findFirstByUserIdOrderByCreatedAtAsc(userId)).thenReturn(Optional.of(existing));
        when(workspaceService.get(workspaceId)).thenReturn(workspace(workspaceId));

        OnboardingCompleteResponse response = service.complete(request());

        assertThat(response.workspace().id()).isEqualTo(workspaceId);
        verify(workspaceService, never()).create(any());
        verify(userProfileService, never()).completeOnboarding(any(), any());
    }

    @Test
    void skippedUserWithExistingProjectShouldReuseItAndFinishOnboarding() {
        UUID workspaceId = UUID.randomUUID();
        profile.setOnboardingStatus(OnboardingStatus.SKIPPED);
        WorkspaceEntity existing = new WorkspaceEntity();
        existing.setId(workspaceId);
        when(workspaceRepository.findFirstByUserIdOrderByCreatedAtAsc(userId)).thenReturn(Optional.of(existing));
        when(workspaceService.get(workspaceId)).thenReturn(workspace(workspaceId));

        OnboardingCompleteResponse response = service.complete(request());

        assertThat(response.workspace().id()).isEqualTo(workspaceId);
        verify(workspaceService, never()).create(any());
        verify(userProfileService).completeOnboarding(profile, "v1");
    }

    private OnboardingCompleteRequest request() {
        return new OnboardingCompleteRequest(new CreateWorkspaceRequest("新研究项目"), "v1");
    }

    private WorkspaceResponse workspace(UUID id) {
        return new WorkspaceResponse(id, "新研究项目", com.aipm.cowriting.domain.model.WorkspaceStatus.DRAFT,
                null, java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now());
    }
}
