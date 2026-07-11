package com.aipm.cowriting.domain.repository;

import com.aipm.cowriting.domain.entity.AcademicDocumentEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicDocumentRepository extends JpaRepository<AcademicDocumentEntity, UUID> {
    List<AcademicDocumentEntity> findByWorkspaceIdOrderByUpdatedAtDesc(UUID workspaceId);
    Optional<AcademicDocumentEntity> findFirstByWorkspaceIdAndPrimaryDocumentTrue(UUID workspaceId);
    boolean existsByWorkspaceId(UUID workspaceId);
}
