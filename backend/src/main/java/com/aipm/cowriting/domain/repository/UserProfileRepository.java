package com.aipm.cowriting.domain.repository;

import com.aipm.cowriting.domain.entity.UserProfileEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfileEntity, UUID> {
}
