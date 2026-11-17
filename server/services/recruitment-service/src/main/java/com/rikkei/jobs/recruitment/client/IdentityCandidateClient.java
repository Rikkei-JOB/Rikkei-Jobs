package com.rikkei.jobs.recruitment.client;

import java.util.UUID;
import java.util.List;
import java.util.Map;

public interface IdentityCandidateClient {
    CandidateIdentity get(UUID userId);
    Map<UUID, CandidateIdentity> getBatch(List<UUID> userIds);
}
