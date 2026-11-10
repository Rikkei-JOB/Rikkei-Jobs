package com.rikkei.jobs.profile.dto;

import com.rikkei.jobs.profile.entity.EvidenceSource;
import com.rikkei.jobs.profile.entity.SkillLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SkillProfileView(SkillInfo skill, BigDecimal verifiedScore, BigDecimal selfScore,
                               SkillLevel level, List<EvidenceView> evidences) {
    public record SkillInfo(UUID id, String code, String name, String group) { }

    public record EvidenceView(EvidenceSource source, String title, BigDecimal score,
                               Instant recordedAt, boolean verified) { }
}
