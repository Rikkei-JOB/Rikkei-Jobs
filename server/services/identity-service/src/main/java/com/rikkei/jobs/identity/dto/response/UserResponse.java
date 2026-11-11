package com.rikkei.jobs.identity.dto.response;

import com.rikkei.jobs.common.config.security.principle.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private UUID id;
    private String email;
    private String fullName;
    private Role role;
    private UUID companyId;
    private Boolean mustChangePassword;
}
