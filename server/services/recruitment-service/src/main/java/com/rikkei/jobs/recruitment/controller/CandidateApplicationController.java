package com.rikkei.jobs.recruitment.controller;

import com.rikkei.jobs.common.config.security.principle.*;
import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.recruitment.dto.request.*;
import com.rikkei.jobs.recruitment.dto.response.*;
import com.rikkei.jobs.recruitment.entity.ApplicationStatus;
import com.rikkei.jobs.recruitment.service.ApplicationService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/recruitment") @RequiredArgsConstructor
@PreAuthorize(Roles.CANDIDATE)
public class CandidateApplicationController {
    private final ApplicationService service;
    @PostMapping("/jobs/{jobId}/apply")
    public ResponseEntity<ApplicationDetailResponse> apply(@AuthenticationPrincipal AuthPrincipal actor,
        @RequestHeader(HttpHeaders.AUTHORIZATION) String bearer, @PathVariable UUID jobId, @Valid @RequestBody ApplyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.apply(actor, bearer, jobId, request));
    }
    @GetMapping("/me/applications")
    public PageResponse<ApplicationSummaryResponse> list(@AuthenticationPrincipal AuthPrincipal actor,
        @RequestParam(required=false) ApplicationStatus status, @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int size) {
        return service.listOwn(actor.getUserId(), status, page, size);
    }
    @GetMapping("/me/applications/{id}")
    public ApplicationDetailResponse detail(@AuthenticationPrincipal AuthPrincipal actor, @PathVariable UUID id) {
        return service.getOwn(actor.getUserId(), id);
    }
    @PostMapping("/me/applications/{id}/withdraw")
    public ApplicationDetailResponse withdraw(@AuthenticationPrincipal AuthPrincipal actor, @PathVariable UUID id, @Valid @RequestBody WithdrawRequest request) {
        return service.withdraw(actor, id, request);
    }
}
