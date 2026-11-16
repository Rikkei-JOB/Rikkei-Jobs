package com.rikkei.jobs.profile.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CertificateRequest(@NotBlank @Size(max = 255) String name,
    @NotBlank @Size(max = 255) String issuer, @NotNull LocalDate issuedAt,
    LocalDate expiresAt, @Size(max = 1000) String fileUrl) {
    public CertificateRequest {
        name = trim(name); issuer = trim(issuer); fileUrl = trim(fileUrl);
    }
    @JsonIgnore @AssertTrue(message = "Ngày hết hạn phải bằng hoặc sau ngày cấp")
    public boolean isDateRangeValid() {
        return issuedAt == null || expiresAt == null || !expiresAt.isBefore(issuedAt);
    }
    private static String trim(String value) { return value == null || value.isBlank() ? null : value.strip(); }
}
