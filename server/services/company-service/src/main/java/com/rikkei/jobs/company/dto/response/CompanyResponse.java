package com.rikkei.jobs.company.dto.response;

import com.rikkei.jobs.company.entity.CompanyStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompanyResponse {
    private UUID id;
    private String name;
    private String taxCode;
    private String industry;
    private String size;
    private String address;
    private String city;
    private String website;
    private String logoUrl;
    private String description;
    private CompanyStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
