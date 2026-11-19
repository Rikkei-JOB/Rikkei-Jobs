package com.rikkei.jobs.recruitment.controller;
import com.rikkei.jobs.common.config.security.principle.*;
import com.rikkei.jobs.common.config.exception.BadRequestException;
import com.rikkei.jobs.recruitment.dto.request.*;
import com.rikkei.jobs.recruitment.dto.response.*;
import com.rikkei.jobs.recruitment.service.InterviewSchedulingService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/recruitment/me/interviews") @RequiredArgsConstructor @PreAuthorize(Roles.CANDIDATE)
public class CandidateInterviewController {
    private final InterviewSchedulingService service;
    @PostMapping("/{roundId}/pick")
    public InterviewScheduleResponse pick(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable UUID roundId,@RequestBody PickInterviewRequest request){if(request.slotId()==null)throw new BadRequestException("Thiếu khung giờ.");return service.pick(actor,roundId,request.slotId());}
}
