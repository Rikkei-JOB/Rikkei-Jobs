package com.rikkei.jobs.recruitment.controller;
import com.rikkei.jobs.recruitment.service.ContactRequestService;
import com.rikkei.jobs.recruitment.dto.contact.ContactDtos.*;
import com.rikkei.jobs.recruitment.entity.ContactRequestStatus;
import com.rikkei.jobs.common.config.security.principle.*;
import com.rikkei.jobs.common.dto.response.PageResponse;
import java.util.UUID;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
@RestController @RequestMapping("/staff/contact-requests") @RequiredArgsConstructor @PreAuthorize(Roles.STAFF)
public class StaffContactRequestController {
    private final ContactRequestService requests;
    @GetMapping public PageResponse<ContactRequestView> list(@RequestParam(required=false) ContactRequestStatus status,
        @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {return requests.list(status,page,size);}
    @PostMapping("/{id}/approve") public ContactRequestView approve(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable UUID id) {
        return requests.review(actor,id,true,null);
    }
    @PostMapping("/{id}/reject") public ContactRequestView reject(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable UUID id,
        @Valid @RequestBody RejectRequest input) {return requests.review(actor,id,false,input.reason());}
}
