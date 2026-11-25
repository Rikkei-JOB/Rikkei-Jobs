package com.rikkei.jobs.company.dto.response;

import com.rikkei.jobs.company.entity.CompanyStatus;
import java.time.Instant;
import java.util.UUID;

/** Một dòng trong danh sách công ty của cán bộ. {@code jdCount} là số JD không ở trạng thái DRAFT. */
public record StaffCompanyItem(UUID id, String name, String taxCode, CompanyStatus status,
                               long hrCount, long jdCount, Instant createdAt,
                               String logoUrl, String city, String size) {
}
