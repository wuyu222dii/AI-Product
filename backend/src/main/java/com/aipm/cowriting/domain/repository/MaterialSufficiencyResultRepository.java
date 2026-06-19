package com.aipm.cowriting.domain.repository;

import com.aipm.cowriting.domain.entity.MaterialSufficiencyResultEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialSufficiencyResultRepository extends JpaRepository<MaterialSufficiencyResultEntity, UUID> {
    Optional<MaterialSufficiencyResultEntity> findFirstByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);
}
