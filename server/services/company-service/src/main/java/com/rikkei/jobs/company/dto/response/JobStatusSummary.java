package com.rikkei.jobs.company.dto.response;

import com.rikkei.jobs.company.entity.JobStatus;
import java.util.Map;

/** Tổng hợp JD của công ty theo trạng thái; {@code expiringSoon} là JD APPROVED hết hạn trong 7 ngày tới. */
public record JobStatusSummary(long total, Map<JobStatus, Long> byStatus, long expiringSoon) {
}
