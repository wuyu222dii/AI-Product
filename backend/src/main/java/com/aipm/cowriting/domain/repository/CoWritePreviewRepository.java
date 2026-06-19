package com.aipm.cowriting.domain.repository;

import com.aipm.cowriting.domain.entity.CoWritePreviewEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoWritePreviewRepository extends JpaRepository<CoWritePreviewEntity, UUID> {
}
