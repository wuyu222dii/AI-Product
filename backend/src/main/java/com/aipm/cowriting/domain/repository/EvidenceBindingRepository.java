package com.aipm.cowriting.domain.repository;

import com.aipm.cowriting.domain.entity.EvidenceBindingEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceBindingRepository extends JpaRepository<EvidenceBindingEntity, UUID> {
    List<EvidenceBindingEntity> findByDraftVersionIdOrderByCreatedAtAsc(UUID draftVersionId);

    void deleteByDraftVersionId(UUID draftVersionId);
}
