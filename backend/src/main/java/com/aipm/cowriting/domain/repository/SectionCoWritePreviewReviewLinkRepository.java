package com.aipm.cowriting.domain.repository;

import com.aipm.cowriting.domain.entity.SectionCoWritePreviewReviewLinkEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectionCoWritePreviewReviewLinkRepository
        extends JpaRepository<SectionCoWritePreviewReviewLinkEntity, UUID> {

    List<SectionCoWritePreviewReviewLinkEntity> findBySectionCowritePreviewIdOrderByCreatedAtAsc(UUID previewId);
}
