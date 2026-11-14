package com.rikkei.jobs.profile.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Roles;
import com.rikkei.jobs.profile.dto.CandidateProfileResponse;
import com.rikkei.jobs.profile.service.ProfileAvatarService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/profiles/me/avatar") @RequiredArgsConstructor
@PreAuthorize(Roles.CANDIDATE)
public class ProfileAvatarController {
    private final ProfileAvatarService avatars;
    @PutMapping
    public CandidateProfileResponse update(@AuthenticationPrincipal AuthPrincipal principal, @Valid @RequestBody AvatarRequest request) {
        return avatars.update(principal.getUserId(), request.fileUrl());
    }
    public record AvatarRequest(@Size(max = 200) String fileUrl) { }
}
