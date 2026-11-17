package com.rikkei.jobs.recruitment.client;

import java.util.UUID;

public record CandidateIdentity(UUID userId, String fullName, String email, String phone) {
    public CandidateIdentity(UUID userId, String fullName, String email) {
        this(userId, fullName, email, null);
    }
}
