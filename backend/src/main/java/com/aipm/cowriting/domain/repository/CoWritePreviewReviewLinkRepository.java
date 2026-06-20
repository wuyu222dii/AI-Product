package com.aipm.cowriting.domain.repository;

import com.aipm.cowriting.domain.entity.CoWritePreviewReviewLinkEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoWritePreviewReviewLinkRepository extends JpaRepository<CoWritePreviewReviewLinkEntity, UUID> {

    List<CoWritePreviewReviewLinkEntity> findByCoWritePreviewId(UUID coWritePreviewId);
}
