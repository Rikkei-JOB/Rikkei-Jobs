package com.rikkei.jobs.recruitment.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Roles;
import com.rikkei.jobs.recruitment.dto.response.EmploymentRecordResponse;
import com.rikkei.jobs.recruitment.service.EmploymentRecordReadService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/recruitment/me/employment-records") @RequiredArgsConstructor
@PreAuthorize(Roles.CANDIDATE)
public class CandidateEmploymentController {
    private final EmploymentRecordReadService records;

    @GetMapping
    public List<EmploymentRecordResponse> list(@AuthenticationPrincipal AuthPrincipal actor) {
        return records.listOwn(actor.getUserId());
    }
}
