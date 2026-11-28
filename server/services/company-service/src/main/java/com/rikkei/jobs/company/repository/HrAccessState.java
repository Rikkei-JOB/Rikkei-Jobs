package com.rikkei.jobs.company.repository;

import com.rikkei.jobs.company.entity.CompanyStatus;
import java.time.Instant;

/** Trạng thái truy cập của một HR: bản sao khoá riêng ({@code lockedAt}) và trạng thái công ty của HR đó. */
public record HrAccessState(Instant lockedAt, CompanyStatus companyStatus) {
}
