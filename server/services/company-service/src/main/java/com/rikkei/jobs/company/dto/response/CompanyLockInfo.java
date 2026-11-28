package com.rikkei.jobs.company.dto.response;

import java.time.Instant;

/** Thông tin khoá công ty lấy từ dòng LOCK mới nhất của company_actions (cho banner chi tiết công ty). */
public record CompanyLockInfo(Instant lockedAt, String reason, StaffRef lockedBy) {
}
