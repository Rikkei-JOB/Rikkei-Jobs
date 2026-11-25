package com.rikkei.jobs.profile.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Roles;
import com.rikkei.jobs.profile.dto.PortfolioProjectRequest;
import com.rikkei.jobs.profile.dto.PortfolioProjectResponse;
import com.rikkei.jobs.profile.service.PortfolioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** HS-03: portfolio dự án thực tập IOC và dự án cá nhân. */
@RestController @RequestMapping("/profiles") @RequiredArgsConstructor
public class PortfolioController {
    private final PortfolioService service;

    @GetMapping("/me/portfolio") @PreAuthorize(Roles.CANDIDATE)
    public List<PortfolioProjectResponse> mine(@AuthenticationPrincipal AuthPrincipal p) { return service.mine(p.getUserId()); }

    @PostMapping("/me/portfolio") @PreAuthorize(Roles.CANDIDATE) @ResponseStatus(HttpStatus.CREATED)
    public PortfolioProjectResponse create(@AuthenticationPrincipal AuthPrincipal p, @Valid @RequestBody PortfolioProjectRequest r) {
        return service.create(p.getUserId(), r);
    }

    @PutMapping("/me/portfolio/order") @PreAuthorize(Roles.CANDIDATE)
    public List<PortfolioProjectResponse> reorder(@AuthenticationPrincipal AuthPrincipal p, @Valid @RequestBody OrderRequest r) {
        return service.reorder(p.getUserId(), r.ids());
    }

    @PutMapping("/me/portfolio/{id}") @PreAuthorize(Roles.CANDIDATE)
    public PortfolioProjectResponse update(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID id,
        @Valid @RequestBody PortfolioProjectRequest r) {
        return service.update(p.getUserId(), id, r);
    }

    @DeleteMapping("/me/portfolio/{id}") @PreAuthorize(Roles.CANDIDATE) @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID id) { service.delete(p.getUserId(), id); }

    @PostMapping(value = "/me/portfolio/{id}/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(Roles.CANDIDATE) @ResponseStatus(HttpStatus.CREATED)
    public PortfolioProjectResponse.Media addImage(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID id,
        @RequestPart("file") MultipartFile file, @RequestParam(value = "caption", required = false) @Size(max = 255) String caption) {
        return service.addImage(p.getUserId(), id, file, caption);
    }

    @DeleteMapping("/me/portfolio/{id}/media/{mediaId}") @PreAuthorize(Roles.CANDIDATE) @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMedia(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID id, @PathVariable UUID mediaId) {
        service.deleteMedia(p.getUserId(), id, mediaId);
    }

    @GetMapping("/{userId}/portfolio") @PreAuthorize(Roles.AUTHENTICATED)
    public List<PortfolioProjectResponse> ofUser(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID userId) {
        return service.ofUser(userId, p.getUserId());
    }

    @GetMapping("/{userId}/portfolio/media/{mediaId}") @PreAuthorize(Roles.AUTHENTICATED)
    public ResponseEntity<byte[]> image(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID userId, @PathVariable UUID mediaId) {
        var file = service.image(userId, p.getUserId(), mediaId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(file.filename()).build().toString())
            .header(HttpHeaders.CACHE_CONTROL, "private, no-store").header("X-Content-Type-Options", "nosniff")
            .body(file.bytes());
    }

    public record OrderRequest(@NotNull @Size(max = 200) List<@NotNull UUID> ids) { }
}
