package com.rikkei.jobs.identity.repository;

import com.rikkei.jobs.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);

    Optional<User> findByLmsUserId(String lmsUserId);

    boolean existsByEmail(String email);

    /** Trang quản trị: mỗi dòng là [role, số tài khoản]. */
    @Query("select u.role, count(u) from User u group by u.role")
    List<Object[]> countByRole();

    /** Số tài khoản bị cán bộ/quản trị khoá (lockedAt khác null), không tính khoá tạm do nhập sai mật khẩu. */
    long countByLockedAtIsNotNull();
}
