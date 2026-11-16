package com.rikkei.jobs.profile.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Roles;
import com.rikkei.jobs.profile.dto.CandidateProfileResponse;
import com.rikkei.jobs.profile.dto.UpdateCandidateProfileRequest;
import com.rikkei.jobs.profile.service.CandidateProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profiles/me")
@RequiredArgsConstructor
@PreAuthorize(Roles.CANDIDATE)
public class CandidateProfileController {
    private final CandidateProfileService service;
    @GetMapping
    public CandidateProfileResponse get(@AuthenticationPrincipal AuthPrincipal principal) {
        return service.get(principal.getUserId());
    }
    @PutMapping
    public CandidateProfileResponse update(@AuthenticationPrincipal AuthPrincipal principal,
        @Valid @RequestBody UpdateCandidateProfileRequest request) {
        return service.update(principal.getUserId(), request);
    }
}
