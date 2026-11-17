package com.rikkei.jobs.company.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.UUID;

/** Trạng thái tài khoản do identity-service trả về (status: LOCKED hoặc ACTIVE). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IdentityUserStatus(UUID userId, String email, String fullName, String status,
                                 String lockedReason, Instant lockedAt, Instant tempLockedUntil) {
}
