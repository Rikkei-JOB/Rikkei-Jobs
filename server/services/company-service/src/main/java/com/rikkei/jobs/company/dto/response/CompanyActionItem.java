package com.rikkei.jobs.company.dto.response;

import com.rikkei.jobs.company.entity.CompanyActionType;
import java.time.Instant;
import java.util.UUID;

/** Một dòng lịch sử thao tác; {@code job} null với LOCK/UNLOCK, {@code reason} null khi thao tác không có lý do. */
public record CompanyActionItem(UUID id, CompanyActionType action, Instant createdAt, String reason,
                                StaffRef staff, JobRef job) {
}
