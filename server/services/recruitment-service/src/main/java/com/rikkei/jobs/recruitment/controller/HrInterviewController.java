package com.rikkei.jobs.recruitment.controller;
import com.rikkei.jobs.common.config.security.principle.*;
import com.rikkei.jobs.recruitment.dto.request.*;
import com.rikkei.jobs.recruitment.dto.response.*;
import com.rikkei.jobs.recruitment.service.InterviewSchedulingService;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/recruitment") @RequiredArgsConstructor @PreAuthorize(Roles.HR)
public class HrInterviewController {
    private final InterviewSchedulingService service;
    @PostMapping("/rounds/{roundId}/slots") @ResponseStatus(HttpStatus.CREATED)
    public InterviewProposalResponse propose(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable UUID roundId,@RequestBody List<InterviewSlotRequest> slots){return service.propose(actor,roundId,slots);}
    @PostMapping("/rounds/{roundId}/reschedule")
    public InterviewProposalResponse reschedule(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable UUID roundId,@RequestBody List<InterviewSlotRequest> slots){return service.reschedule(actor,roundId,slots);}
}
