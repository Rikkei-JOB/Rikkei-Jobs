package com.rikkei.jobs.profile.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Roles;
import com.rikkei.jobs.profile.dto.*;
import com.rikkei.jobs.profile.service.ProfileRecordsService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/profiles/me") @RequiredArgsConstructor @PreAuthorize(Roles.CANDIDATE)
public class ProfileRecordsController {
    private final ProfileRecordsService service;
    @GetMapping("/educations")
    public List<EducationResponse> educations(@AuthenticationPrincipal AuthPrincipal p) { return service.educations(p.getUserId()); }
    @PostMapping("/educations") @ResponseStatus(HttpStatus.CREATED)
    public EducationResponse createEducation(@AuthenticationPrincipal AuthPrincipal p, @Valid @RequestBody EducationRequest r) {
        return service.createEducation(p.getUserId(), r);
    }
    @PutMapping("/educations/{id}")
    public EducationResponse updateEducation(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID id, @Valid @RequestBody EducationRequest r) {
        return service.updateEducation(p.getUserId(), id, r);
    }
    @DeleteMapping("/educations/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEducation(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID id) { service.deleteEducation(p.getUserId(), id); }
    @GetMapping("/certificates")
    public List<CertificateResponse> certificates(@AuthenticationPrincipal AuthPrincipal p) { return service.certificates(p.getUserId()); }
    @PostMapping("/certificates") @ResponseStatus(HttpStatus.CREATED)
    public CertificateResponse createCertificate(@AuthenticationPrincipal AuthPrincipal p, @Valid @RequestBody CertificateRequest r) {
        return service.createCertificate(p.getUserId(), r);
    }
    @PutMapping("/certificates/{id}")
    public CertificateResponse updateCertificate(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID id, @Valid @RequestBody CertificateRequest r) {
        return service.updateCertificate(p.getUserId(), id, r);
    }
    @DeleteMapping("/certificates/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCertificate(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID id) { service.deleteCertificate(p.getUserId(), id); }
}
