package com.rikkei.jobs.recruitment.client;

import java.util.UUID;

public interface CandidateContactClient {
    String phone(UUID userId, String bearer);
}
