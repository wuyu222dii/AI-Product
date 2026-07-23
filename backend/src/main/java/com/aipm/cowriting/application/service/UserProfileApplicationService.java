package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.user.CurrentUserResponse;
import com.aipm.cowriting.application.dto.user.UpdateCurrentUserRequest;
import com.aipm.cowriting.application.dto.user.UpdateOnboardingRequest;
import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.common.error.ErrorCode;
import com.aipm.cowriting.domain.entity.UserProfileEntity;
import com.aipm.cowriting.domain.model.OnboardingStatus;
import com.aipm.cowriting.domain.repository.UserProfileRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileApplicationService {

    private final UserProfileRepository repository;
    private final CurrentUserService currentUser;

    public UserProfileApplicationService(UserProfileRepository repository, CurrentUserService currentUser) {
        this.repository = repository;
        this.currentUser = currentUser;
    }

    @Transactional
    public CurrentUserResponse getCurrent() {
        return toResponse(getOrCreate());
    }

    @Transactional
    public CurrentUserResponse update(UpdateCurrentUserRequest request) {
        UserProfileEntity profile = getOrCreate();
        profile.setDisplayName(request.displayName().trim());
        profile.setUpdatedAt(OffsetDateTime.now());
        return toResponse(repository.save(profile));
    }

    @Transactional
    public CurrentUserResponse updateOnboarding(UpdateOnboardingRequest request) {
        if (request.status() == OnboardingStatus.NOT_STARTED) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST_BODY,
                    HttpStatus.BAD_REQUEST.value(),
                    "onboarding 状态只能更新为 COMPLETED 或 SKIPPED"
            );
        }
        UserProfileEntity profile = getOrCreate();
        applyOnboardingStatus(profile, request.status(), request.onboardingVersion());
        return toResponse(repository.save(profile));
    }

    @Transactional
    public UserProfileEntity lockCurrentProfile() {
        UserProfileEntity profile = getOrCreate();
        return repository.findByIdForUpdate(profile.getId()).orElse(profile);
    }

    public CurrentUserResponse completeOnboarding(UserProfileEntity profile, String version) {
        applyOnboardingStatus(profile, OnboardingStatus.COMPLETED, version);
        return toResponse(repository.save(profile));
    }

    private UserProfileEntity getOrCreate() {
        UUID userId = currentUser.userId();
        return repository.findById(userId).orElseGet(() -> {
            OffsetDateTime now = OffsetDateTime.now();
            UserProfileEntity profile = new UserProfileEntity();
            profile.setId(userId);
            profile.setDisplayName(currentUser.suggestedDisplayName());
            profile.setAvatarUrl(currentUser.suggestedAvatarUrl());
            profile.setOnboardingStatus(OnboardingStatus.NOT_STARTED);
            profile.setCreatedAt(now);
            profile.setUpdatedAt(now);
            return repository.save(profile);
        });
    }

    private CurrentUserResponse toResponse(UserProfileEntity profile) {
        return new CurrentUserResponse(
                profile.getId(), currentUser.email(), profile.getDisplayName(), profile.getAvatarUrl(),
                profile.getOnboardingStatus(), profile.getOnboardingVersion(), profile.getOnboardingCompletedAt(),
                profile.getCreatedAt(), profile.getUpdatedAt()
        );
    }

    private void applyOnboardingStatus(
            UserProfileEntity profile,
            OnboardingStatus status,
            String version
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        profile.setOnboardingStatus(status);
        profile.setOnboardingVersion(version == null || version.isBlank() ? "v1" : version.trim());
        profile.setOnboardingCompletedAt(now);
        profile.setUpdatedAt(now);
    }
}
