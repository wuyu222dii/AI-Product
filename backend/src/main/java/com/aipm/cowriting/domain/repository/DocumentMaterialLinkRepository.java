package com.aipm.cowriting.domain.repository;

import com.aipm.cowriting.domain.entity.DocumentMaterialLinkEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentMaterialLinkRepository extends JpaRepository<DocumentMaterialLinkEntity, UUID> {
    List<DocumentMaterialLinkEntity> findByDocumentIdOrderByCreatedAtAsc(UUID documentId);
    List<DocumentMaterialLinkEntity> findByDocumentIdAndIncludedTrue(UUID documentId);
    Optional<DocumentMaterialLinkEntity> findByDocumentIdAndMaterialId(UUID documentId, UUID materialId);
    boolean existsByDocumentId(UUID documentId);
}
