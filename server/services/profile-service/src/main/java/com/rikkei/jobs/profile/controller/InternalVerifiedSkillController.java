package com.rikkei.jobs.profile.controller;

import com.rikkei.jobs.profile.repository.SkillScoreRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
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

/** Internal lookup of a candidate's verified skill scores, used by company-service for job matching. */
@RestController
@RequestMapping("/internal/profiles")
public class InternalVerifiedSkillController {
    public record VerifiedSkill(UUID skillId, BigDecimal verifiedScore, Instant updatedAt) {}
    private final SkillScoreRepository scores;
    private final String key;

    public InternalVerifiedSkillController(SkillScoreRepository scores,
        @Value("${app.profile.internal-service-key:}") String key) {
        this.scores = scores; this.key = key;
    }

    @GetMapping("/{userId}/verified-skills")
    @PreAuthorize("isAuthenticated()")
    public List<VerifiedSkill> verifiedSkills(@PathVariable UUID userId,
        @RequestHeader(value = "X-Internal-Key", required = false) String provided) {
        if (key == null || key.isBlank() || provided == null || !MessageDigest.isEqual(
            key.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal service key");
        }
        return scores.findAllByIdUserId(userId).stream()
            .filter(s -> s.getVerifiedScore() != null)
            .map(s -> new VerifiedSkill(s.getId().getSkillId(), s.getVerifiedScore(), s.getUpdatedAt()))
            .toList();
    }
}
