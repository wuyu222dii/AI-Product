package com.aipm.cowriting.domain.repository;

import com.aipm.cowriting.domain.entity.AppealCaseEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppealCaseRepository extends JpaRepository<AppealCaseEntity, UUID> {
    Optional<AppealCaseEntity> findByReviewItemId(UUID reviewItemId);
}
