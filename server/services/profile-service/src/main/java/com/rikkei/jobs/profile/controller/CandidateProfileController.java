package com.rikkei.jobs.profile.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Roles;
import com.rikkei.jobs.profile.dto.CandidateProfileResponse;
import com.rikkei.jobs.profile.dto.UpdateCandidateProfileRequest;
import com.rikkei.jobs.profile.service.CandidateProfileService;
import com.rikkei.jobs.common.config.exception.BadRequestException;
import com.rikkei.jobs.profile.entity.ProfileReview;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
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
    public record PublishRequest(@Schema(type = "boolean", allowableValues = "true", requiredMode = Schema.RequiredMode.REQUIRED)
                                JsonNode consent) {}

    @PostMapping("/publish")
    public CandidateProfileResponse publish(@AuthenticationPrincipal AuthPrincipal principal,
                                            @RequestBody PublishRequest request) {
        if (request.consent() == null || !request.consent().isBoolean() || !request.consent().booleanValue()) {
            throw new BadRequestException("Ứng viên cần đồng ý công khai hồ sơ.");
        }
        return service.publish(principal.getUserId());
    }

    @PostMapping("/withdraw")
    public CandidateProfileResponse withdraw(@AuthenticationPrincipal AuthPrincipal principal) {
        return service.withdraw(principal.getUserId());
    }

    @GetMapping("/review-history")
    public List<ProfileReview> history(@AuthenticationPrincipal AuthPrincipal principal) {
        return service.history(principal.getUserId());
    }

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
