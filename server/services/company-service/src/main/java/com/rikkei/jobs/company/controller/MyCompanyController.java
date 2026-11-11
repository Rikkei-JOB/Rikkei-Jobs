package com.rikkei.jobs.company.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Roles;
import com.rikkei.jobs.company.dto.request.UpdateCompanyRequest;
import com.rikkei.jobs.company.dto.response.CompanyResponse;
import com.rikkei.jobs.company.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Công ty chỉ được xác định từ userId của JWT (qua company_hr), không đọc claim companyId hay id nào trong body. */
@RestController
@RequestMapping("/companies/me")
@RequiredArgsConstructor
public class MyCompanyController {

    private final CompanyService companyService;

    @GetMapping
    @PreAuthorize(Roles.HR)
    public CompanyResponse get(@AuthenticationPrincipal AuthPrincipal principal) {
        return companyService.getMine(principal.getUserId());
    }

    @PutMapping
    @PreAuthorize(Roles.HR)
    public CompanyResponse update(@Valid @RequestBody UpdateCompanyRequest request,
                                  @AuthenticationPrincipal AuthPrincipal principal) {
        return companyService.updateMine(principal.getUserId(), request);
    }
}
