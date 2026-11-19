package com.rikkei.jobs.recruitment.controller;
import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.recruitment.dto.response.MatchLookupResponse;
import com.rikkei.jobs.recruitment.service.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/recruitment/applications") @RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR','STAFF')")
public class ApplicationMatchController {
    private final ApplicationReadAccessService access;
    private final MatchLookupService matches;
    @GetMapping("/{id}/match")
    public MatchLookupResponse match(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable UUID id,
        @RequestHeader(HttpHeaders.AUTHORIZATION) String bearer) {
        var application=access.findReadable(actor,id);
        return matches.lookup(application.getJobId(),application.getUserId(),bearer);
    }
}
