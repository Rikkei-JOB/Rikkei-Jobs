package com.rikkei.jobs.identity.dto.response;

import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.identity.entity.User;

import java.time.Instant;
import java.util.UUID;

/**
 * Trạng thái tài khoản cho company-service. {@code status} là giá trị suy ra: LOCKED khi bị khoá riêng (lockedAt khác null)
 * hoặc khoá theo công ty ({@code companyLocked}), ngược lại ACTIVE. {@code tempLockedUntil} chỉ có khi đang khoá tạm (sai mật khẩu) còn hạn.
 */
public record InternalUserStatusResponse(
        UUID userId,
        String email,
        String fullName,
        Role role,
        UUID companyId,
        String status,
        String lockedReason,
        Instant lockedAt,
        Instant tempLockedUntil,
        boolean companyLocked) {

    public static InternalUserStatusResponse from(User user, Instant now) {
        boolean adminLocked = user.isAdminLocked();
        Instant until = user.getLockedUntil();
        Instant temp = !adminLocked && until != null && until.isAfter(now) ? until : null;
        return new InternalUserStatusResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getCompanyId(),
                user.isLoginBlocked() ? "LOCKED" : "ACTIVE",
                user.getLockedReason(),
                user.getLockedAt(),
                temp,
                user.isCompanyLocked());
    }
}
