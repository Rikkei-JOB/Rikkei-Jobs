package com.rikkei.jobs.company.client;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/** Điểm kỹ năng đã xác thực của ứng viên, lấy từ profile-service. */
public interface ProfileSkillClient {

    /** GET /internal/profiles/{userId}/verified-skills; trả map skillId -> điểm (rỗng nếu chưa có). */
    Map<UUID, BigDecimal> verifiedScores(UUID userId, String authorization);
}
