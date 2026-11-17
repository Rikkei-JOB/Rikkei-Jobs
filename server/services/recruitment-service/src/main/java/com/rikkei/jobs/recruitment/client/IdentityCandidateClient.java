package com.rikkei.jobs.recruitment.client;

import java.util.UUID;

public interface IdentityCandidateClient {
    CandidateIdentity get(UUID userId);
}
