package com.rikkei.jobs.profile.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Roles;
import com.rikkei.jobs.profile.dto.SkillProfileView;
import com.rikkei.jobs.profile.service.SkillProfileAccessService;
import com.rikkei.jobs.profile.service.SkillProfileService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profiles")
public class SkillProfileController {
    private final SkillProfileAccessService access;
    private final SkillProfileService skills;

    public SkillProfileController(SkillProfileAccessService access, SkillProfileService skills) {
        this.access = access;
        this.skills = skills;
    }

    @GetMapping("/me/skills")
    @PreAuthorize(Roles.CANDIDATE)
    public List<SkillProfileView> ownSkills(@AuthenticationPrincipal AuthPrincipal principal) {
        return access.getSkills(principal.getUserId(), principal.getUserId());
    }

    @GetMapping("/{userId}/skills")
    @PreAuthorize("isAuthenticated()")
    public List<SkillProfileView> profileSkills(@PathVariable UUID userId,
                                               @AuthenticationPrincipal AuthPrincipal principal) {
        return access.getSkills(userId, principal.getUserId());
    }

    @PostMapping("/me/skills/recompute")
    @PreAuthorize(Roles.CANDIDATE)
    public List<SkillProfileView> recomputeOwnSkills(@AuthenticationPrincipal AuthPrincipal principal) {
        return skills.recomputeSkills(principal.getUserId());
    }
}
