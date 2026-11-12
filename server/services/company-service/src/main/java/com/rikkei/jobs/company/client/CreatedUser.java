package com.rikkei.jobs.company.client;

import java.util.UUID;

/** Tài khoản HR vừa được identity-service tạo. */
public record CreatedUser(UUID userId, String email, String fullName, UUID companyId) {
}
