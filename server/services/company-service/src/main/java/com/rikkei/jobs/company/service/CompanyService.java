package com.rikkei.jobs.company.service;

import com.rikkei.jobs.company.dto.request.CreateCompanyRequest;
import com.rikkei.jobs.company.dto.request.UpdateCompanyRequest;
import com.rikkei.jobs.company.dto.response.CompanyResponse;
import com.rikkei.jobs.company.entity.Company;
import java.util.UUID;

public interface CompanyService {

    /** Cán bộ tạo công ty mới (trạng thái ACTIVE); trùng mã số thuế thì ném ConflictException. */
    CompanyResponse create(CreateCompanyRequest request, UUID staffUserId);

    /** Công ty của HR, tra chỉ theo userId qua company_hr; không có thì ném ResourceNotFoundException. */
    Company findMine(UUID userId);

    CompanyResponse getMine(UUID userId);

    /** Thay toàn bộ trường cho phép sửa; công ty LOCKED thì ném CompanyException(COMPANY_LOCKED). */
    CompanyResponse updateMine(UUID userId, UpdateCompanyRequest request);
}
