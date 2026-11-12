package com.rikkei.jobs.company.dto.response;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HrAccountResponse {
    private UUID userId;
    private String email;
    private String fullName;
    private UUID companyId;
}
