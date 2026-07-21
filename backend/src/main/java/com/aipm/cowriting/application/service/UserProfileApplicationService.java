package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.user.CurrentUserResponse;
import com.aipm.cowriting.application.dto.user.UpdateCurrentUserRequest;
import com.aipm.cowriting.domain.entity.UserProfileEntity;
import com.aipm.cowriting.domain.repository.UserProfileRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
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

    private UserProfileEntity getOrCreate() {
        UUID userId = currentUser.userId();
        return repository.findById(userId).orElseGet(() -> {
            OffsetDateTime now = OffsetDateTime.now();
            UserProfileEntity profile = new UserProfileEntity();
            profile.setId(userId);
            profile.setDisplayName(currentUser.suggestedDisplayName());
            profile.setAvatarUrl(currentUser.suggestedAvatarUrl());
            profile.setCreatedAt(now);
            profile.setUpdatedAt(now);
            return repository.save(profile);
        });
    }

    private CurrentUserResponse toResponse(UserProfileEntity profile) {
        return new CurrentUserResponse(
                profile.getId(), currentUser.email(), profile.getDisplayName(), profile.getAvatarUrl(),
                profile.getCreatedAt(), profile.getUpdatedAt()
        );
    }
}
