package com.aipm.cowriting.domain.repository;

import com.aipm.cowriting.domain.entity.WorkspaceEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<WorkspaceEntity, UUID> {
}
