package com.rikkei.jobs.identity.dto;

import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.identity.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Quản trị tài khoản (ADMIN): /admin/users/**. */
public final class AdminUserDtos {
    private AdminUserDtos() { }

    /** status: LOCKED khi bị cán bộ/quản trị khoá; khoá tạm do nhập sai mật khẩu vẫn tính ACTIVE. */
    public record AdminUserItem(UUID id, String email, String fullName, Role role, String lmsUserId, UUID companyId,
                                String status, String lockedReason, Instant lockedAt, Instant createdAt) {
        public static AdminUserItem from(User u) {
            return new AdminUserItem(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.getLmsUserId(), u.getCompanyId(),
                u.isAdminLocked() ? "LOCKED" : "ACTIVE", u.getLockedReason(), u.getLockedAt(), u.getCreatedAt());
        }
    }

    public record AdminUserSummary(long total, long locked, Map<Role, Long> byRole) { }

    public record LockUserRequest(
        @NotBlank(message = "Vui lòng nhập lý do khoá.")
        @Size(max = 500, message = "Lý do tối đa 500 ký tự.") String reason) {
        public LockUserRequest { reason = reason == null ? null : reason.strip(); }
    }
}
