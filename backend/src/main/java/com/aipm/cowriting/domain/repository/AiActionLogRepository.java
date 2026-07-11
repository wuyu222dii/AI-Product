package com.aipm.cowriting.domain.repository;

import com.aipm.cowriting.domain.entity.AiActionLogEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiActionLogRepository extends JpaRepository<AiActionLogEntity, UUID> {
    List<AiActionLogEntity> findByDocumentIdOrderByCreatedAtDesc(UUID documentId);
}
