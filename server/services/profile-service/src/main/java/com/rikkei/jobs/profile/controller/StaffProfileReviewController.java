package com.rikkei.jobs.profile.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Roles;
import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.profile.dto.CandidateProfileResponse;
import com.rikkei.jobs.profile.entity.ProfileReview.Action;
import com.rikkei.jobs.profile.service.CandidateProfileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/staff")
@RequiredArgsConstructor
@PreAuthorize(Roles.STAFF)
public class StaffProfileReviewController {
    private final CandidateProfileService service;
    public record RejectRequest(@NotBlank String reason) {}

    @GetMapping("/profile-reviews")
    public PageResponse<CandidateProfileResponse> list(@RequestParam(defaultValue = "PENDING") String status,
        @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return service.reviewQueue(status, page, size);
    }

    @PostMapping("/profiles/{userId}/approve")
    public CandidateProfileResponse approve(@PathVariable UUID userId, @AuthenticationPrincipal AuthPrincipal staff) {
        return service.review(userId, staff.getUserId(), Action.APPROVE, null);
    }

    @PostMapping("/profiles/{userId}/reject")
    public CandidateProfileResponse reject(@PathVariable UUID userId, @AuthenticationPrincipal AuthPrincipal staff,
                                          @Valid @RequestBody RejectRequest request) {
        return service.review(userId, staff.getUserId(), Action.REJECT, request.reason().strip());
    }
}
