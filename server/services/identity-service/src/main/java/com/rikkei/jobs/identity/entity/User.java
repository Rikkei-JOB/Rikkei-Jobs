package com.rikkei.jobs.identity.entity;

import com.rikkei.jobs.common.config.security.principle.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "email", length = 255, unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "full_name", length = 255, nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "lms_user_id", length = 100, unique = true)
    private String lmsUserId;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "must_change_password", nullable = false)
    private Boolean mustChangePassword;

    @Column(name = "failed_login_count", nullable = false)
    private Integer failedLoginCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "locked_reason", length = 500)
    private String lockedReason;

    // Khác null nghĩa là tài khoản bị cán bộ khoá (khác với khoá tạm do sai mật khẩu).
    @Column(name = "locked_at")
    private Instant lockedAt;

    // Khác null nghĩa là công ty của HR đang bị cán bộ khoá (độc lập với khoá riêng ở locked_at).
    @Column(name = "company_locked_at")
    private Instant companyLockedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private UserStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (this.mustChangePassword == null) {
            this.mustChangePassword = false;
        }
        if (this.failedLoginCount == null) {
            this.failedLoginCount = 0;
        }
        if (this.status == null) {
            this.status = UserStatus.ACTIVE;
        }
    }

    public boolean isAdminLocked() {
        return lockedAt != null;
    }

    public boolean isCompanyLocked() {
        return companyLockedAt != null;
    }

    /** Bị chặn đăng nhập/token do khoá riêng hoặc khoá theo công ty. */
    public boolean isLoginBlocked() {
        return isAdminLocked() || isCompanyLocked();
    }

    public boolean isLockedAt(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }
}
