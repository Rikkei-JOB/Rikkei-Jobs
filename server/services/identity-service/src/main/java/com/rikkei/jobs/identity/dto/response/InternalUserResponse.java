package com.rikkei.jobs.identity.dto.response;

import com.rikkei.jobs.common.config.security.principle.Role;

import java.util.UUID;

public record InternalUserResponse(UUID userId, String email, String fullName, Role role, UUID companyId) {
}
