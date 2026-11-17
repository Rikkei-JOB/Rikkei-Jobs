package com.rikkei.jobs.company.dto.response;

import java.time.Instant;
import java.util.UUID;

/** Tài khoản HR dưới góc nhìn cán bộ; status là LOCKED (cán bộ khoá) hoặc ACTIVE. */
public record StaffHrAccount(UUID userId, String email, String fullName, String status,
                             String lockedReason, Instant lockedAt, Instant tempLockedUntil) {
}
