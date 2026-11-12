package com.rikkei.jobs.company.service;

import com.rikkei.jobs.company.dto.request.CreateHrAccountRequest;
import com.rikkei.jobs.company.dto.response.HrAccountResponse;
import java.util.UUID;

public interface HrAccountService {

    /**
     * Cấp tài khoản HR cho công ty qua identity-service rồi ghi liên kết company_hr. Công ty không có thì 404,
     * bị khoá thì COMPANY_LOCKED, email đã dùng thì 409 (trừ trường hợp nối lại HR chưa được liên kết).
     */
    HrAccountResponse create(UUID companyId, CreateHrAccountRequest request);
}
