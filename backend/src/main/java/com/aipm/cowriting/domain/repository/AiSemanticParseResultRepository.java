package com.aipm.cowriting.domain.repository;

import com.aipm.cowriting.domain.entity.AiSemanticParseResultEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiSemanticParseResultRepository extends JpaRepository<AiSemanticParseResultEntity, UUID> {
    Optional<AiSemanticParseResultEntity> findByMaterialId(UUID materialId);
    List<AiSemanticParseResultEntity> findByMaterialIdIn(List<UUID> materialIds);
}
