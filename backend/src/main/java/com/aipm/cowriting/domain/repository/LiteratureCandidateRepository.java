package com.aipm.cowriting.domain.repository;

import com.aipm.cowriting.domain.entity.LiteratureCandidateEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LiteratureCandidateRepository extends JpaRepository<LiteratureCandidateEntity, UUID> {
    List<LiteratureCandidateEntity> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);

    Optional<LiteratureCandidateEntity> findFirstByWorkspaceIdAndDuplicateGroupKey(UUID workspaceId, String duplicateGroupKey);
}
