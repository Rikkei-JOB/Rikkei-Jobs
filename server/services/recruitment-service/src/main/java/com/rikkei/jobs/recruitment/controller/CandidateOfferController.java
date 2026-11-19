package com.rikkei.jobs.recruitment.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Roles;
import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.recruitment.dto.request.DeclineOfferRequest;
import com.rikkei.jobs.recruitment.dto.response.OfferResponse;
import com.rikkei.jobs.recruitment.service.OfferCommandService;
import com.rikkei.jobs.recruitment.service.OfferQueryService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/recruitment/me/offers") @RequiredArgsConstructor
@PreAuthorize(Roles.CANDIDATE)
public class CandidateOfferController {
    private final OfferCommandService commands;
    private final OfferQueryService queries;

    @GetMapping
    public PageResponse<OfferResponse> list(@AuthenticationPrincipal AuthPrincipal actor,
        @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return queries.listOwn(actor.getUserId(), page, size);
    }

    @GetMapping("/{id}")
    public OfferResponse detail(@AuthenticationPrincipal AuthPrincipal actor, @PathVariable UUID id) {
        return queries.getOwn(actor.getUserId(), id);
    }

    @PostMapping("/{id}/accept")
    public OfferResponse accept(@AuthenticationPrincipal AuthPrincipal actor, @PathVariable UUID id) {
        return commands.accept(actor, id);
    }

    @PostMapping("/{id}/decline")
    public OfferResponse decline(@AuthenticationPrincipal AuthPrincipal actor, @PathVariable UUID id,
        @RequestBody DeclineOfferRequest request) {
        return commands.decline(actor, id, request == null ? null : request.reason());
    }
}
