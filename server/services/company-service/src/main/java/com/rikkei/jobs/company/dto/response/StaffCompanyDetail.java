package com.rikkei.jobs.company.dto.response;

import com.rikkei.jobs.company.entity.CompanyStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Chi tiết công ty cho cán bộ: mọi field của {@link CompanyResponse} cộng danh sách tài khoản HR. */
public record StaffCompanyDetail(UUID id, String name, String taxCode, String industry, String size,
                                 String address, String city, String website, String logoUrl,
                                 String description, CompanyStatus status, Instant createdAt,
                                 Instant updatedAt, List<StaffHrAccount> hrAccounts) {
}
