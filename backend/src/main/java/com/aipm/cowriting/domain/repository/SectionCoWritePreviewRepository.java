package com.aipm.cowriting.domain.repository;

import com.aipm.cowriting.domain.entity.SectionCoWritePreviewEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectionCoWritePreviewRepository extends JpaRepository<SectionCoWritePreviewEntity, UUID> {
}
