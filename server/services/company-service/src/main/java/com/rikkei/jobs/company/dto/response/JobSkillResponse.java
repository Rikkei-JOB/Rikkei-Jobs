package com.rikkei.jobs.company.dto.response;

import java.util.UUID;

/** Kỹ năng yêu cầu của JD; backend không trả tên, frontend ghép với danh mục GET /skills. */
public record JobSkillResponse(UUID skillId, boolean required, int minScore) {
}
