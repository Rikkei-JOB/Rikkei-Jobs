package com.rikkei.jobs.common.config.security.principle;

import lombok.Getter;

import java.util.UUID;

/**
 * Người đang gọi API, lấy từ JWT (sub = userId, role, companyId, mustChangePassword).
 * Dùng trong controller: {@code @AuthenticationPrincipal AuthPrincipal principal}.
 */
@Getter
public class AuthPrincipal {
    private final UUID userId;
    private final Role role;
    private final UUID companyId;
    /** true khi HR chưa đổi mật khẩu bắt buộc: mọi service (trừ /auth/**) trả 403 MUST_CHANGE_PASSWORD. */
    private final boolean mustChangePassword;

    public AuthPrincipal(UUID userId, Role role, UUID companyId, boolean mustChangePassword) {
        this.userId = userId;
        this.role = role;
        this.companyId = companyId;
        this.mustChangePassword = mustChangePassword;
    }

    public AuthPrincipal(UUID userId, Role role, UUID companyId) {
        this(userId, role, companyId, false);
    }
}
