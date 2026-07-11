package com.aipm.cowriting.domain.repository;

import com.aipm.cowriting.domain.entity.AcademicProjectProfileEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicProjectProfileRepository extends JpaRepository<AcademicProjectProfileEntity, UUID> {
}
