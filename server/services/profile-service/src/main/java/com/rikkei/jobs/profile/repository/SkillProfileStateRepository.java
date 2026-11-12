package com.rikkei.jobs.profile.repository;

import com.rikkei.jobs.profile.entity.SkillProfileState;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SkillProfileStateRepository extends JpaRepository<SkillProfileState, UUID> {
    @Modifying
    @Query(value = "INSERT INTO profile.skill_profile_states(user_id, consented, approved) "
            + "VALUES (:userId, false, false) ON CONFLICT DO NOTHING", nativeQuery = true)
    void ensureExists(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from SkillProfileState p where p.userId = :userId")
    Optional<SkillProfileState> lockByUserId(@Param("userId") UUID userId);
}
