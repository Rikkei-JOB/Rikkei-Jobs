package com.rikkei.jobs.profile.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Roles;
import com.rikkei.jobs.profile.service.ProfileFileService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController @RequestMapping("/files") @RequiredArgsConstructor
@PreAuthorize(Roles.CANDIDATE)
public class ProfileFileController {
    private final ProfileFileService files;
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FileResponse upload(@AuthenticationPrincipal AuthPrincipal principal, @RequestPart("file") MultipartFile file) {
        return new FileResponse(files.upload(principal.getUserId(), file));
    }
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> download(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
        var file = files.download(principal.getUserId(), id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.filename()).build().toString())
            .header(HttpHeaders.CACHE_CONTROL, "private, no-store").header("X-Content-Type-Options", "nosniff")
            .body(file.bytes());
    }
    public record FileResponse(String url) { }
}
