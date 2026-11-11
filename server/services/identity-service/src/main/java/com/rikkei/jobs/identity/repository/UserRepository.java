package com.rikkei.jobs.identity.repository;

import com.rikkei.jobs.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    Optional<User> findByLmsUserId(String lmsUserId);

    boolean existsByEmail(String email);
}
