package com.rikkei.jobs.recruitment.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Roles;
import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.recruitment.dto.request.UpdateRoundRequest;
import com.rikkei.jobs.recruitment.dto.response.InterviewRoundResponse;
import com.rikkei.jobs.recruitment.dto.response.MentorRoundResponse;
import com.rikkei.jobs.recruitment.service.InterviewRoundService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/recruitment/mentor/rounds") @RequiredArgsConstructor
@PreAuthorize(Roles.MENTOR)
public class MentorRoundController {
    private final InterviewRoundService rounds;

    @GetMapping
    public PageResponse<MentorRoundResponse> list(@AuthenticationPrincipal AuthPrincipal actor,
        @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return rounds.listInvited(actor, page, size);
    }

    @PutMapping("/{roundId}")
    public InterviewRoundResponse update(@AuthenticationPrincipal AuthPrincipal actor,
        @PathVariable UUID roundId, @Valid @RequestBody UpdateRoundRequest request) {
        return rounds.updateInvited(actor, roundId, request);
    }
}
