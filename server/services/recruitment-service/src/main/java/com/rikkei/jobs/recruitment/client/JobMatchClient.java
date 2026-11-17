package com.rikkei.jobs.recruitment.client;

import com.rikkei.jobs.recruitment.dto.response.MatchResponse;
import java.util.Optional;
import java.util.UUID;

public interface JobMatchClient {
    Optional<MatchResponse> get(UUID jobId, UUID userId, String bearer);
}
