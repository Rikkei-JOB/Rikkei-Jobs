package com.rikkei.jobs.company.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.UUID;

/**
 * Trạng thái tài khoản do identity-service trả về. {@code status} là LOCKED khi bị khoá riêng hoặc khoá theo công ty,
 * ngược lại ACTIVE; {@code companyLocked} cho biết riêng cờ khoá theo công ty (DN-07), null khi identity cũ chưa trả.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IdentityUserStatus(UUID userId, String email, String fullName, String status,
                                 String lockedReason, Instant lockedAt, Instant tempLockedUntil,
                                 Boolean companyLocked) {
}
