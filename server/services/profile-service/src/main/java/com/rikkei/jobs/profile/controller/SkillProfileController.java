package com.rikkei.jobs.profile.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Roles;
import com.rikkei.jobs.profile.dto.SkillProfileView;
import com.rikkei.jobs.profile.service.SkillProfileAccessService;
import com.rikkei.jobs.profile.service.SkillProfileService;
import com.rikkei.jobs.profile.service.sync.SyncService;
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
    private final SyncService sync;

    public SkillProfileController(SkillProfileAccessService access, SkillProfileService skills, SyncService sync) {
        this.access = access;
        this.skills = skills;
        this.sync = sync;
    }

    @GetMapping("/me/skills")
    @PreAuthorize(Roles.CANDIDATE)
    public List<SkillProfileView> ownSkills(@AuthenticationPrincipal AuthPrincipal principal) {
        // NT-05: lần đồng bộ cuối quá 1 giờ thì đồng bộ ở luồng nền; trang trả ngay với dữ liệu hiện có.
        sync.syncInBackgroundIfStale(principal.getUserId());
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
