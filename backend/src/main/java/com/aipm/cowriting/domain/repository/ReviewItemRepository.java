package com.aipm.cowriting.domain.repository;

import com.aipm.cowriting.domain.entity.ReviewItemEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewItemRepository extends JpaRepository<ReviewItemEntity, UUID> {
    List<ReviewItemEntity> findByDraftVersionIdOrderByCreatedAtAsc(UUID draftVersionId);
    void deleteByDraftVersionId(UUID draftVersionId);
}
