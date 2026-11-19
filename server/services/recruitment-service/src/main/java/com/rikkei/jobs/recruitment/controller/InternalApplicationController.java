package com.rikkei.jobs.recruitment.controller;

import com.rikkei.jobs.common.config.exception.BadRequestException;
import com.rikkei.jobs.recruitment.repository.ApplicationRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Internal check used by company-service: has this candidate applied to this job (any status)? */
@RestController
@RequestMapping("/internal/applications")
public class InternalApplicationController {
    public record Exists(boolean applied) {}
    private final ApplicationRepository applications;
    private final String key;

    public InternalApplicationController(ApplicationRepository applications,
        @Value("${app.recruitment.internal-service-key:}") String key) {
        this.applications = applications; this.key = key;
    }

    @GetMapping("/exists")
    @PreAuthorize("hasAnyRole('HR','STAFF')")
    public Exists exists(@RequestParam(required = false) UUID jobId, @RequestParam(required = false) UUID userId,
        @RequestHeader(value = "X-Internal-Key", required = false) String provided) {
        if (key == null || key.isBlank() || provided == null || !MessageDigest.isEqual(
            key.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal service key");
        }
        if (jobId == null || userId == null) throw new BadRequestException("Thiếu jobId hoặc userId");
        return new Exists(applications.existsByJobIdAndUserId(jobId, userId));
    }
}
