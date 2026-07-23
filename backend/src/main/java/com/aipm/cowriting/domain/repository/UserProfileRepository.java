package com.aipm.cowriting.domain.repository;

import com.aipm.cowriting.domain.entity.UserProfileEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserProfileRepository extends JpaRepository<UserProfileEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select profile from UserProfileEntity profile where profile.id = :id")
    Optional<UserProfileEntity> findByIdForUpdate(@Param("id") UUID id);
}
