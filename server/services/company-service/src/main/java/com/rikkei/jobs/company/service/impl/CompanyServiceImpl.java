package com.rikkei.jobs.company.service.impl;

import com.rikkei.jobs.common.config.security.access.OwnershipGuard;
import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.company.dto.response.CompanyResponse;
import com.rikkei.jobs.company.mapper.CompanyMapper;
import com.rikkei.jobs.company.repository.CompanyRepository;
import com.rikkei.jobs.company.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    static final String NOT_FOUND = "Không tìm thấy công ty";

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;

    @Override
    @Transactional(readOnly = true)
    public CompanyResponse getCompany(AuthPrincipal principal, UUID companyId) {
        // Kiểm sở hữu trước khi đọc DB: công ty của người khác và công ty không tồn tại trả cùng một lỗi 404.
        OwnershipGuard.requireCompany(principal, companyId, NOT_FOUND);
        return companyRepository.findById(companyId)
                .map(companyMapper::toResponse)
                .orElseThrow(() -> OwnershipGuard.notFound(NOT_FOUND));
    }
}
