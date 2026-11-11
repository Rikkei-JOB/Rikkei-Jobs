package com.rikkei.jobs.company.service;

import com.rikkei.jobs.company.dto.request.CreateCompanyRequest;
import com.rikkei.jobs.company.dto.response.CompanyResponse;
import java.util.UUID;

public interface CompanyService {

    /** Cán bộ tạo công ty mới (trạng thái ACTIVE); trùng mã số thuế thì ném ConflictException. */
    CompanyResponse create(CreateCompanyRequest request, UUID staffUserId);
}
