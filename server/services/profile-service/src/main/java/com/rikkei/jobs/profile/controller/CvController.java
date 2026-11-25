package com.rikkei.jobs.profile.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Roles;
import com.rikkei.jobs.profile.dto.CvDtos.*;
import com.rikkei.jobs.profile.service.CvPdfRenderer;
import com.rikkei.jobs.profile.service.CvService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * HS-04: CV của ứng viên. Đường /profile/cvs khớp hợp đồng UT-01 (recruitment-service gọi /profile/cvs/{id}/pdf
 * với JWT ứng viên khi nộp đơn) và route gateway /api/v1/profile/**.
 */
@RestController @RequestMapping("/profile") @RequiredArgsConstructor
public class CvController {
    private static final String NO_STORE = "private, no-store";
    private final CvService service;
    private final CvPdfRenderer renderer;

    @GetMapping("/cvs") @PreAuthorize(Roles.CANDIDATE)
    public ResponseEntity<List<CvSummary>> list(@AuthenticationPrincipal AuthPrincipal p) {
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, NO_STORE).body(service.list(p.getUserId()));
    }

    @PostMapping("/cvs") @PreAuthorize(Roles.CANDIDATE) @ResponseStatus(HttpStatus.CREATED)
    public CvDetail create(@AuthenticationPrincipal AuthPrincipal p, @Valid @RequestBody CvRequest r) {
        return service.create(p.getUserId(), r);
    }

    @GetMapping("/cvs/{id}") @PreAuthorize(Roles.CANDIDATE)
    public CvDetail get(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID id) { return service.get(p.getUserId(), id); }

    @PutMapping("/cvs/{id}") @PreAuthorize(Roles.CANDIDATE)
    public CvDetail update(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID id, @Valid @RequestBody CvRequest r) {
        return service.update(p.getUserId(), id, r);
    }

    @DeleteMapping("/cvs/{id}") @PreAuthorize(Roles.CANDIDATE) @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID id) { service.delete(p.getUserId(), id); }

    @PutMapping("/cvs/{id}/sections") @PreAuthorize(Roles.CANDIDATE)
    public CvDetail sections(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID id, @Valid @RequestBody SectionsRequest r) {
        return service.replaceSections(p.getUserId(), id, r);
    }

    /** Chỉ chủ CV. inline=true để khung xem trước nhúng PDF, mặc định tải về. */
    @GetMapping(value = "/cvs/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE) @PreAuthorize(Roles.CANDIDATE)
    public ResponseEntity<byte[]> pdf(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID id,
        @RequestParam(defaultValue = "false") boolean inline) {
        byte[] bytes = renderer.render(service.render(p.getUserId(), id));
        var disposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment()).filename("cv-" + id + ".pdf").build();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .header(HttpHeaders.CACHE_CONTROL, NO_STORE).header("X-Content-Type-Options", "nosniff")
            .body(bytes);
    }

    /** Trang công khai mà QR trỏ tới. Không cần đăng nhập (xem ProfileSecurityConfig). */
    @GetMapping("/public/{token}")
    public ResponseEntity<PublicProfile> publicProfile(@PathVariable String token) {
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(service.publicProfile(token));
    }
}
