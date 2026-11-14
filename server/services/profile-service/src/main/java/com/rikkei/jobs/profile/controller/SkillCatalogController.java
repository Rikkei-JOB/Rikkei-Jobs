package com.rikkei.jobs.profile.controller;

import com.rikkei.jobs.profile.dto.SkillProfileView.SkillInfo;
import com.rikkei.jobs.profile.service.SkillProfileService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/skills") @RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class SkillCatalogController {
    private final SkillProfileService skills;
    @GetMapping
    public List<SkillInfo> catalogue() { return skills.getCatalogue(); }
}
