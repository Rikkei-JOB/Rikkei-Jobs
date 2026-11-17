package com.rikkei.jobs.recruitment.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.recruitment.dto.response.HrApplicationDetailResponse;
import com.rikkei.jobs.recruitment.service.HrApplicationReadService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/recruitment/applications") @RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR','STAFF')")
public class ApplicationDetailController {
    private final HrApplicationReadService service;
    @GetMapping("/{id}")
    public HrApplicationDetailResponse detail(@AuthenticationPrincipal AuthPrincipal actor,
        @PathVariable UUID id, @RequestHeader(HttpHeaders.AUTHORIZATION) String bearer) {
        return service.detail(actor, id, bearer);
    }
}
