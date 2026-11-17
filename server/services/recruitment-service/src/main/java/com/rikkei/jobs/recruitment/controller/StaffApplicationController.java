package com.rikkei.jobs.recruitment.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.recruitment.dto.response.HrApplicationSummaryResponse;
import com.rikkei.jobs.recruitment.entity.ApplicationStatus;
import com.rikkei.jobs.recruitment.service.HrApplicationReadService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/recruitment/staff/applications") @RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF','ADMIN')")
public class StaffApplicationController {
    private final HrApplicationReadService service;
    @GetMapping
    public PageResponse<HrApplicationSummaryResponse> list(@AuthenticationPrincipal AuthPrincipal actor,
        @RequestParam(required = false) UUID companyId, @RequestParam(required = false) UUID jobId,
        @RequestParam(required = false) ApplicationStatus status, @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size) {
        return service.listStaff(actor, companyId, jobId, status, page, size);
    }
}
