package com.rikkei.jobs.company.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Roles;
import com.rikkei.jobs.company.dto.request.UpdateCompanyRequest;
import com.rikkei.jobs.company.dto.response.CompanyResponse;
import com.rikkei.jobs.company.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Qua gateway: {@code GET /api/v1/companies/{id}} (NT-02). API của HR với công ty mình ở {@code MyCompanyController} (DN-01). */
@Tag(name = "Doanh nghiệp")
@RestController
@RequestMapping("/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @Operation(
            summary = "Xem thông tin công ty",
            description = "Vai trò: HR, STAFF, ADMIN. HR chỉ xem được công ty mình được liên kết (company_hr); công ty khác trả 404 như khi không tồn tại.")
    @ApiResponse(responseCode = "200", description = "Thông tin công ty")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: thiếu hoặc sai token")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: vai trò không được gọi API này")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: không có công ty, hoặc không phải công ty của HR đang gọi")
    @GetMapping("/{id}")
    @PreAuthorize(Roles.HR_STAFF_ADMIN)
    public CompanyResponse getCompany(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
        return companyService.getCompany(principal, id);
    }

    @Operation(
            summary = "Sửa thông tin công ty theo id",
            description = "Vai trò: HR (DN-01). Chỉ sửa được công ty mình được liên kết (company_hr), xử lý y như "
                    + "PUT /companies/me; công ty khác hoặc không tồn tại trả 404 như nhau. Không sửa được mã số thuế, "
                    + "trạng thái, logo.")
    @ApiResponse(responseCode = "200", description = "Thông tin công ty sau khi lưu")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: dữ liệu không hợp lệ (errors theo trường)")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: thiếu hoặc sai token; ACCOUNT_DISABLED: HR bị khoá")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: không phải HR; COMPANY_LOCKED: công ty đang bị khoá")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: không có công ty, hoặc không phải công ty của HR đang gọi")
    @PutMapping("/{id}")
    @PreAuthorize(Roles.HR)
    public CompanyResponse updateCompany(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id,
                                         @Valid @RequestBody UpdateCompanyRequest request) {
        return companyService.updateCompany(principal.getUserId(), id, request);
    }
}
