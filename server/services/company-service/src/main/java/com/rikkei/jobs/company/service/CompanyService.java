package com.rikkei.jobs.company.service;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.company.dto.response.CompanyResponse;

import java.util.UUID;

public interface CompanyService {

    /** HR chỉ xem được công ty của mình; công ty khác trả 404 như khi không tồn tại. STAFF/ADMIN xem mọi công ty. */
    CompanyResponse getCompany(AuthPrincipal principal, UUID companyId);
}
