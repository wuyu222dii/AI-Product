package com.aipm.cowriting.domain.repository;

import com.aipm.cowriting.domain.entity.DocumentSectionEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentSectionRepository extends JpaRepository<DocumentSectionEntity, UUID> {
    List<DocumentSectionEntity> findByDocumentIdOrderBySortOrderAsc(UUID documentId);
    int countByDocumentId(UUID documentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select section from DocumentSectionEntity section where section.id = :id")
    Optional<DocumentSectionEntity> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select section from DocumentSectionEntity section where section.documentId = :documentId order by section.id")
    List<DocumentSectionEntity> findByDocumentIdForUpdate(@Param("documentId") UUID documentId);
}
