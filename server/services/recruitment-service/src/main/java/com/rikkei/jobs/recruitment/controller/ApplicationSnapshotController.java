package com.rikkei.jobs.recruitment.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.recruitment.service.ApplicationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/recruitment/applications") @RequiredArgsConstructor
public class ApplicationSnapshotController {
    private final ApplicationService service;
    @GetMapping(value="/{id}/cv", produces=MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('CANDIDATE','HR')")
    public ResponseEntity<byte[]> cv(@AuthenticationPrincipal AuthPrincipal actor, @PathVariable UUID id) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cv-" + id + ".pdf\"")
            .body(service.readSnapshot(actor, id));
    }
}
