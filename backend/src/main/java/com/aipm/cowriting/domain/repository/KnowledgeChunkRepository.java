package com.aipm.cowriting.domain.repository;

import com.aipm.cowriting.domain.entity.KnowledgeChunkEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunkEntity, UUID> {
    void deleteByWorkspaceId(UUID workspaceId);

    List<KnowledgeChunkEntity> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);

    long countByWorkspaceId(UUID workspaceId);
}
