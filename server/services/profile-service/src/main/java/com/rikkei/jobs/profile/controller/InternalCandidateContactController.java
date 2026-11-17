package com.rikkei.jobs.profile.controller;

import com.rikkei.jobs.profile.repository.CandidateProfileRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Minimal internal contact lookup after recruitment has checked application access. */
@RestController
@RequestMapping("/internal/profiles")
public class InternalCandidateContactController {
    public record Contact(UUID userId, String phone) {}
    private final CandidateProfileRepository profiles;
    private final String key;

    public InternalCandidateContactController(CandidateProfileRepository profiles,
        @Value("${app.profile.internal-service-key:}") String key) {
        this.profiles = profiles; this.key = key;
    }

    @GetMapping("/{userId}/phone")
    @PreAuthorize("hasAnyRole('HR','STAFF','ADMIN')")
    public Contact phone(@PathVariable UUID userId,
        @RequestHeader(value = "X-Internal-Key", required = false) String provided) {
        if (key == null || key.isBlank() || provided == null || !MessageDigest.isEqual(
            key.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal service key");
        }
        return new Contact(userId, profiles.findById(userId).map(p -> p.getPhone()).orElse(null));
    }
}
