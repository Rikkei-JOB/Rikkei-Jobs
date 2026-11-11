package com.rikkei.jobs.company.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Roles;
import com.rikkei.jobs.company.dto.response.CompanyResponse;
import com.rikkei.jobs.company.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Qua gateway: {@code /api/v1/company/companies/...}. DN-01 bổ sung các API quản lý doanh nghiệp tại đây. */
@Tag(name = "Doanh nghiệp")
@RestController
@RequestMapping("/company/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @Operation(
            summary = "Xem thông tin công ty",
            description = "Vai trò: HR, STAFF, ADMIN. HR chỉ xem được công ty của mình; công ty khác trả 404 như khi không tồn tại.")
    @ApiResponse(responseCode = "200", description = "Thông tin công ty")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: thiếu hoặc sai token")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: vai trò không được gọi API này")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: không có công ty, hoặc không phải công ty của HR đang gọi")
    @GetMapping("/{id}")
    @PreAuthorize(Roles.HR_STAFF_ADMIN)
    public CompanyResponse getCompany(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
        return companyService.getCompany(principal, id);
    }
}
