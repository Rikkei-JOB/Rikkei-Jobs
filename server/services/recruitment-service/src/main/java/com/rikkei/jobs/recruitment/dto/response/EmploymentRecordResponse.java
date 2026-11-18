package com.rikkei.jobs.recruitment.dto.response;

import com.rikkei.jobs.recruitment.entity.EmploymentRecord;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EmploymentRecordResponse(UUID id, UUID applicationId, UUID userId, UUID companyId,
    UUID jobId, String companyName, String position, LocalDate startDate, Instant confirmedAt) {
    public static EmploymentRecordResponse from(EmploymentRecord record, String companyName) {
        return new EmploymentRecordResponse(record.getId(), record.getApplicationId(), record.getUserId(),
            record.getCompanyId(), record.getJobId(), companyName, record.getPosition(),
            record.getStartDate(), record.getConfirmedAt());
    }
}
