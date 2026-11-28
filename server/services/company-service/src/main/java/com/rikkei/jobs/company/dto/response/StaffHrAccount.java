package com.rikkei.jobs.company.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Tài khoản HR dưới góc nhìn cán bộ. {@code status} chỉ nói về khoá riêng (LOCKED khi và chỉ khi cán bộ khoá riêng
 * HR này, tức {@code lockedAt} khác null), {@code companyLocked} là cờ khoá theo công ty (DN-07).
 */
public record StaffHrAccount(UUID userId, String email, String fullName, String status,
                             String lockedReason, Instant lockedAt, Instant tempLockedUntil,
                             boolean companyLocked) {
}
