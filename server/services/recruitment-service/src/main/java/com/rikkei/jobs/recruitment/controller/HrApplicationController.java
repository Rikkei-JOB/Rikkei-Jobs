package com.rikkei.jobs.recruitment.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Roles;
import com.rikkei.jobs.recruitment.dto.request.*;
import com.rikkei.jobs.recruitment.dto.response.*;
import com.rikkei.jobs.recruitment.service.HrApplicationWorkflowService;
import com.rikkei.jobs.recruitment.service.InterviewRoundService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/recruitment/applications")
@RequiredArgsConstructor
@PreAuthorize(Roles.HR)
public class HrApplicationController {
    private final HrApplicationWorkflowService workflow;
    private final InterviewRoundService rounds;

    @PostMapping("/{id}/transition")
    public ApplicationWorkflowResponse transition(@AuthenticationPrincipal AuthPrincipal actor,
        @PathVariable UUID id, @Valid @RequestBody TransitionRequest request) {
        return workflow.transition(actor, id, request.toStatus(), request.note());
    }

    @PostMapping("/{id}/rounds")
    @ResponseStatus(HttpStatus.CREATED)
    public InterviewRoundResponse createRound(@AuthenticationPrincipal AuthPrincipal actor,
        @PathVariable UUID id, @Valid @RequestBody CreateRoundRequest request) {
        return rounds.create(actor, id, request);
    }

    @PutMapping("/{id}/rounds/{roundId}")
    public InterviewRoundResponse updateRound(@AuthenticationPrincipal AuthPrincipal actor,
        @PathVariable UUID id, @PathVariable UUID roundId, @Valid @RequestBody UpdateRoundRequest request) {
        return rounds.update(actor, id, roundId, request);
    }
}
