package com.rikkei.jobs.company.entity;

import java.io.Serializable;
import java.util.UUID;

public record MatchScoreId(UUID jobId, UUID userId) implements Serializable {
}
