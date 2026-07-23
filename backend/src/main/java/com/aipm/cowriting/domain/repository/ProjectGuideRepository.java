package com.aipm.cowriting.domain.repository;

import com.aipm.cowriting.domain.entity.ProjectGuideEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectGuideRepository extends JpaRepository<ProjectGuideEntity, UUID> {
}
