package com.rikkei.jobs.profile.controller;

import com.rikkei.jobs.profile.dto.SkillProfileView.SkillInfo;
import com.rikkei.jobs.profile.service.SkillProfileService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Công khai (ProfileSecurityConfig): bảng việc làm hiển thị tên kỹ năng cho khách chưa đăng nhập. */
@RestController @RequestMapping("/skills") @RequiredArgsConstructor
public class SkillCatalogController {
    private final SkillProfileService skills;
    @GetMapping
    public List<SkillInfo> catalogue() { return skills.getCatalogue(); }
}
