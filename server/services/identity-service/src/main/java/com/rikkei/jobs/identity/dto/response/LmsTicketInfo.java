package com.rikkei.jobs.identity.dto.response;

import com.rikkei.jobs.common.config.security.principle.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Danh tính trong vé SSO đã được LMS xác thực. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LmsTicketInfo {
    private String lmsUserId;
    private String email;
    private String fullName;
    private Role role;
    private Instant issuedAt;
}
