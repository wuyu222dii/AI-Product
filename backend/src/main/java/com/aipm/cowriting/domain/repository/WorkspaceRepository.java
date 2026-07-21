package com.aipm.cowriting.domain.repository;

import com.aipm.cowriting.domain.entity.WorkspaceEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<WorkspaceEntity, UUID> {
    List<WorkspaceEntity> findByUserIdOrderByUpdatedAtDesc(UUID userId);
    List<WorkspaceEntity> findByLegacyUnownedTrueOrderByUpdatedAtDesc();
    Optional<WorkspaceEntity> findByIdAndUserId(UUID id, UUID userId);
}
