package com.rikkei.jobs.company.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Roles;
import com.rikkei.jobs.company.dto.request.CreateCompanyRequest;
import com.rikkei.jobs.company.dto.request.CreateHrAccountRequest;
import com.rikkei.jobs.company.dto.response.CompanyResponse;
import com.rikkei.jobs.company.dto.response.HrAccountResponse;
import com.rikkei.jobs.company.service.CompanyService;
import com.rikkei.jobs.company.service.HrAccountService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/staff/companies")
@RequiredArgsConstructor
public class StaffCompanyController {

    private final CompanyService companyService;
    private final HrAccountService hrAccountService;

    @PostMapping
    @PreAuthorize(Roles.STAFF)
    public ResponseEntity<CompanyResponse> create(@Valid @RequestBody CreateCompanyRequest request,
                                                  @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.status(201).body(companyService.create(request, principal.getUserId()));
    }

    @PostMapping("/{id}/hr-accounts")
    @PreAuthorize(Roles.STAFF)
    public ResponseEntity<HrAccountResponse> createHrAccount(@PathVariable UUID id,
                                                             @Valid @RequestBody CreateHrAccountRequest request) {
        return ResponseEntity.status(201).body(hrAccountService.create(id, request));
    }
}
