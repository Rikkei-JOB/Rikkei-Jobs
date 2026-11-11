package com.rikkei.jobs.identity.dto.response;

import java.time.Instant;

public record DevSsoTicketResponse(String ticket, Instant issuedAt) {
}
