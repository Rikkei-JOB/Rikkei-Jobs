package com.rikkei.jobs.recruitment.controller;
import com.rikkei.jobs.recruitment.service.ContactAccessLogService;
import com.rikkei.jobs.recruitment.dto.contact.ContactDtos.ContactAccessLogView;
import com.rikkei.jobs.common.config.security.principle.*;
import com.rikkei.jobs.common.dto.response.PageResponse;
import java.util.UUID;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
@RestController @RequiredArgsConstructor
public class ContactAccessLogController {
    private final ContactAccessLogService logs;
    @GetMapping("/staff/contact-access-logs") @PreAuthorize(Roles.STAFF)
    public ResponseEntity<PageResponse<ContactAccessLogView>> staff(
        @RequestParam(required=false) UUID companyId,@RequestParam(required=false) UUID userId,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
        @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(
            logs.staffLogs(companyId,userId,from==null?null:from.toInstant(),to==null?null:to.toInstant(),page,size));
    }
    @GetMapping("/me/contact-access-logs") @PreAuthorize(Roles.CANDIDATE)
    public ResponseEntity<PageResponse<ContactAccessLogView>> mine(@AuthenticationPrincipal AuthPrincipal actor,
        @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(logs.myLogs(actor,page,size));
    }
}
