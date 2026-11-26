package com.rikkei.jobs.profile.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.profile.dto.WorkProfileDtos.UpdateWorkProfileRequest;
import com.rikkei.jobs.profile.dto.WorkProfileDtos.WorkProfileResponse;
import com.rikkei.jobs.profile.entity.WorkProfile;
import com.rikkei.jobs.profile.repository.WorkProfileRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hồ sơ nghề nghiệp của người đang đăng nhập (Mentor, Staff, Admin), qua gateway: /api/v1/profiles/work/me.
 * Ứng viên có hồ sơ riêng (/profiles/me); HR có hồ sơ công ty. Họ tên, email thuộc identity-service.
 */
@Tag(name = "Hồ sơ nghề nghiệp (Mentor, Staff, Admin)")
@RestController
@RequestMapping("/profiles/work/me")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MENTOR','STAFF','ADMIN')")
public class WorkProfileController {
    private static final String SEPARATOR = "\n";

    private final WorkProfileRepository repository;
    private final Clock clock;

    @Operation(summary = "Xem hồ sơ nghề nghiệp", description = "Vai trò: MENTOR, STAFF, ADMIN. Chưa lưu lần nào thì mọi trường trống, updatedAt null.")
    @ApiResponse(responseCode = "200", description = "Hồ sơ nghề nghiệp")
    @GetMapping
    @Transactional(readOnly = true)
    public WorkProfileResponse get(@AuthenticationPrincipal AuthPrincipal principal) {
        return repository.findById(principal.getUserId()).map(WorkProfileController::toResponse)
            .orElse(new WorkProfileResponse(null, null, null, null, null, List.of(), null, null));
    }

    @Operation(summary = "Cập nhật hồ sơ nghề nghiệp", description = "Vai trò: MENTOR, STAFF, ADMIN. Ghi đè toàn bộ; trường trống gửi null.")
    @ApiResponse(responseCode = "200", description = "Hồ sơ sau khi lưu")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: dữ liệu sai (errors theo field)")
    @PutMapping
    @Transactional
    public WorkProfileResponse update(@AuthenticationPrincipal AuthPrincipal principal,
                                      @Valid @RequestBody UpdateWorkProfileRequest request) {
        WorkProfile profile = repository.findById(principal.getUserId()).orElseGet(() -> {
            WorkProfile created = new WorkProfile();
            created.setUserId(principal.getUserId());
            return created;
        });
        profile.setTitle(request.title());
        profile.setOrganization(request.organization());
        profile.setYearsExperience(request.yearsExperience());
        profile.setPhone(request.phone());
        profile.setLinkedinUrl(request.linkedinUrl());
        profile.setExpertise(request.expertise() == null || request.expertise().isEmpty() ? null : String.join(SEPARATOR, request.expertise()));
        profile.setBio(request.bio());
        profile.setUpdatedAt(Instant.now(clock));
        return toResponse(repository.save(profile));
    }

    private static WorkProfileResponse toResponse(WorkProfile p) {
        List<String> expertise = p.getExpertise() == null ? List.of() : Arrays.stream(p.getExpertise().split(SEPARATOR)).toList();
        return new WorkProfileResponse(p.getTitle(), p.getOrganization(), p.getYearsExperience(), p.getPhone(),
            p.getLinkedinUrl(), expertise, p.getBio(), p.getUpdatedAt());
    }
}
