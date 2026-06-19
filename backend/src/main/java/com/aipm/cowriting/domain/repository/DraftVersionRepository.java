package com.aipm.cowriting.domain.repository;

import com.aipm.cowriting.domain.entity.DraftVersionEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DraftVersionRepository extends JpaRepository<DraftVersionEntity, UUID> {
    Optional<DraftVersionEntity> findFirstByWorkspaceIdOrderByVersionNoDesc(UUID workspaceId);
    List<DraftVersionEntity> findByWorkspaceIdOrderByVersionNoDesc(UUID workspaceId);
}
