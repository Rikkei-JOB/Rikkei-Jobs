package com.rikkei.jobs.profile.dto;

import com.rikkei.jobs.profile.entity.EvidenceSource;
import com.rikkei.jobs.profile.entity.SkillLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SkillProfileView(SkillInfo skill, BigDecimal verifiedScore, BigDecimal selfScore,
                               SkillLevel level, List<EvidenceView> evidences) {
    /** active=false: kỹ năng đã tắt (NT-12), không chọn mới được nhưng dữ liệu cũ vẫn hiện. */
    public record SkillInfo(UUID id, String code, String name, String group, boolean active) { }

    /** id dùng để ứng viên báo dữ liệu sai (NT-12). */
    public record EvidenceView(UUID id, EvidenceSource source, String title, BigDecimal score,
                               Instant recordedAt, boolean verified) { }
}
