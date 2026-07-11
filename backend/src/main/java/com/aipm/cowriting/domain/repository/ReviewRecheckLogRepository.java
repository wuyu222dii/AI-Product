package com.aipm.cowriting.domain.repository;

import com.aipm.cowriting.domain.entity.ReviewRecheckLogEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRecheckLogRepository extends JpaRepository<ReviewRecheckLogEntity, UUID> {
    List<ReviewRecheckLogEntity> findByReviewItemIdOrderByCreatedAtDesc(UUID reviewItemId);

    List<ReviewRecheckLogEntity> findByReviewItemIdInOrderByCreatedAtDesc(Collection<UUID> reviewItemIds);
}
