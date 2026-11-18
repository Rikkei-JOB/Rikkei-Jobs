package com.rikkei.jobs.company.entity;

import java.io.Serializable;
import java.util.UUID;

public record JobSkillId(UUID jobId, UUID skillId) implements Serializable {
}
