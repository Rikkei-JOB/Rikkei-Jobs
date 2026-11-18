package com.rikkei.jobs.recruitment.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Roles;
import com.rikkei.jobs.recruitment.dto.request.CreateOfferRequest;
import com.rikkei.jobs.recruitment.dto.response.OfferResponse;
import com.rikkei.jobs.recruitment.dto.response.EmploymentRecordResponse;
import com.rikkei.jobs.recruitment.service.OfferCommandService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/recruitment/applications") @RequiredArgsConstructor
@PreAuthorize(Roles.HR)
public class HrOfferController {
    private final OfferCommandService offers;

    @PostMapping("/{id}/offer") @ResponseStatus(HttpStatus.CREATED)
    public OfferResponse send(@AuthenticationPrincipal AuthPrincipal actor, @PathVariable UUID id,
        @Valid @RequestBody CreateOfferRequest request) {
        return offers.send(actor, id, request);
    }

    @PostMapping("/{id}/offer/confirm-onboard")
    public EmploymentRecordResponse confirmOnboard(@AuthenticationPrincipal AuthPrincipal actor,
        @PathVariable UUID id) {
        return offers.confirmOnboard(actor, id);
    }
}
