package com.rikkei.jobs.ai.dto.response;

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
public class PingResponse {
    private String service;
    private UUID userId;
    private Role role;
    private UUID companyId;
}
