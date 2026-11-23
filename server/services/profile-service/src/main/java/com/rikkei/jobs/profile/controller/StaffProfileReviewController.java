package com.rikkei.jobs.profile.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Roles;
import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.profile.dto.StaffProfilePreview;
import com.rikkei.jobs.profile.dto.StaffReviewRow;
import com.rikkei.jobs.profile.entity.ProfileReview.Action;
import com.rikkei.jobs.profile.service.CandidateProfileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public record EditRequest(@NotBlank String field, @NotNull JsonNode value, @NotBlank String reason) {}
    public record ReviewResult(String visibility) {}

    @GetMapping("/profile-reviews")
    public PageResponse<StaffReviewRow> list(@RequestParam(defaultValue = "PENDING") String status,
        @RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String classCode,
        @RequestParam(defaultValue = "") String courseName,
        @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return service.reviewQueue(status, q.strip(), classCode.strip(), courseName.strip(), page, size);
    }

    @GetMapping("/profiles/{userId}/preview")
    public StaffProfilePreview preview(@PathVariable UUID userId) {
        return service.preview(userId);
    }

    @PutMapping("/profiles/{userId}/self-declared")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void edit(@PathVariable UUID userId, @AuthenticationPrincipal AuthPrincipal staff,
                     @Valid @RequestBody EditRequest request) {
        service.edit(userId, staff.getUserId(), request.field(), request.value(), request.reason().strip());
    }

    @PostMapping("/profiles/{userId}/approve")
    public ReviewResult approve(@PathVariable UUID userId, @AuthenticationPrincipal AuthPrincipal staff) {
        return new ReviewResult(service.review(userId, staff.getUserId(), Action.APPROVE, null));
    }

    @PostMapping("/profiles/{userId}/reject")
    public ReviewResult reject(@PathVariable UUID userId, @AuthenticationPrincipal AuthPrincipal staff,
                               @Valid @RequestBody RejectRequest request) {
        return new ReviewResult(service.review(userId, staff.getUserId(), Action.REJECT, request.reason().strip()));
    }
}
