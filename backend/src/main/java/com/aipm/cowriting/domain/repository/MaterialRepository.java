package com.aipm.cowriting.domain.repository;

import com.aipm.cowriting.domain.entity.MaterialEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialRepository extends JpaRepository<MaterialEntity, UUID> {
    List<MaterialEntity> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);
}
