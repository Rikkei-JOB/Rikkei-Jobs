package com.rikkei.jobs.company.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Roles;
import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.company.dto.response.JobStatusSummary;
import com.rikkei.jobs.company.dto.response.JobSummary;
import com.rikkei.jobs.company.entity.JobStatus;
import com.rikkei.jobs.company.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Qua gateway: {@code GET /api/v1/companies/me/jobs} (DN-02), cạnh MyCompanyController. */
@Tag(name = "HR - JD")
@RestController
@RequestMapping("/companies/me/jobs")
@RequiredArgsConstructor
public class MyCompanyJobController {

    private final JobService jobService;

    @Operation(
            summary = "Danh sách JD của công ty mình",
            description = "Vai trò: HR. Lọc theo trạng thái (tuỳ chọn); page từ 1, size tối đa 100; mới cập nhật trước.")
    @ApiResponse(responseCode = "200", description = "Trang JD {items, total, page, size}")
    @ApiResponse(responseCode = "400", description = "BAD_REQUEST: page/size hoặc status không hợp lệ")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: thiếu hoặc sai token; ACCOUNT_DISABLED: HR bị khoá")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: không phải HR")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: HR chưa được liên kết công ty")
    @GetMapping
    @PreAuthorize(Roles.HR)
    public PageResponse<JobSummary> list(@RequestParam(required = false) JobStatus status,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "20") int size,
                                         @AuthenticationPrincipal AuthPrincipal principal) {
        return jobService.listMine(principal.getUserId(), status, page, size);
    }

    @Operation(
            summary = "Tổng hợp JD của công ty mình",
            description = "Vai trò: HR. Số JD theo từng trạng thái (đủ 5 khoá) và số JD APPROVED hết hạn trong 7 ngày tới. "
                    + "Công ty LOCKED vẫn xem được.")
    @ApiResponse(responseCode = "200", description = "{total, byStatus, expiringSoon}")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: thiếu hoặc sai token; ACCOUNT_DISABLED: HR bị khoá")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: không phải HR")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: HR chưa được liên kết công ty")
    @GetMapping("/summary")
    @PreAuthorize(Roles.HR)
    public JobStatusSummary summary(@AuthenticationPrincipal AuthPrincipal principal) {
        return jobService.summaryMine(principal.getUserId());
    }
}
