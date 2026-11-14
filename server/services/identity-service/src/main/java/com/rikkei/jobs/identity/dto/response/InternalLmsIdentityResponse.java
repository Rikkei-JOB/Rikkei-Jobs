package com.rikkei.jobs.identity.dto.response;

import java.util.UUID;

public record InternalLmsIdentityResponse(UUID userId, String lmsUserId) {
}
