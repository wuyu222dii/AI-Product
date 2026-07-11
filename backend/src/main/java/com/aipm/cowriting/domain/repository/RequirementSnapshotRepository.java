package com.aipm.cowriting.domain.repository;

import com.aipm.cowriting.domain.entity.RequirementSnapshotEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequirementSnapshotRepository extends JpaRepository<RequirementSnapshotEntity, UUID> {
    Optional<RequirementSnapshotEntity> findFirstByWorkspaceIdOrderByVersionDesc(UUID workspaceId);
    Optional<RequirementSnapshotEntity> findFirstByWorkspaceIdAndDocumentIdOrderByVersionDesc(UUID workspaceId, UUID documentId);
    Optional<RequirementSnapshotEntity> findFirstByWorkspaceIdAndDocumentIdIsNullOrderByVersionDesc(UUID workspaceId);
}
