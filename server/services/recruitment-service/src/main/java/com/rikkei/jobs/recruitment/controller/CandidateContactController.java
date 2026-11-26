package com.rikkei.jobs.recruitment.controller;
import com.rikkei.jobs.recruitment.service.ContactRequestService;
import com.rikkei.jobs.recruitment.dto.contact.ContactDtos.*;
import com.rikkei.jobs.common.config.security.principle.*;
import java.util.UUID;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.*;
@RestController @RequestMapping("/candidates/{userId}") @RequiredArgsConstructor @PreAuthorize(Roles.HR)
public class CandidateContactController {
    private final ContactRequestService requests;
    private final com.rikkei.jobs.recruitment.service.CandidateContactService contacts;
    @GetMapping("/contact")
    public ResponseEntity<ContactView> contact(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable UUID userId,
        @RequestHeader("Authorization") String bearer) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(contacts.reveal(actor,userId,bearer));
    }
    @PostMapping("/contact-requests") @ResponseStatus(HttpStatus.CREATED)
    public ContactRequestView request(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable UUID userId,
        @Valid @RequestBody CreateRequest body,@RequestHeader("Authorization") String bearer) {
        return requests.create(actor,userId,body.jobId(),body.reason(),bearer);
    }
    @GetMapping("/contact-access")
    public ResponseEntity<ContactAccessView> status(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable UUID userId,
        @RequestHeader("Authorization") String bearer) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(requests.status(actor,userId,bearer));
    }
}
