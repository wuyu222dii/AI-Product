package com.aipm.cowriting.domain.repository;

import com.aipm.cowriting.domain.entity.DocumentSectionVersionEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentSectionVersionRepository extends JpaRepository<DocumentSectionVersionEntity, UUID> {
    List<DocumentSectionVersionEntity> findBySectionIdOrderByVersionNoDesc(UUID sectionId);
    Optional<DocumentSectionVersionEntity> findFirstBySectionIdOrderByVersionNoDesc(UUID sectionId);
}
