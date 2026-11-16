package com.rikkei.jobs.profile.dto;

import com.rikkei.jobs.profile.entity.Certificate;
import java.time.LocalDate;
import java.util.UUID;

public record CertificateResponse(UUID id, String name, String issuer, LocalDate issuedAt,
    LocalDate expiresAt, String fileUrl, String fileType) {
    public static CertificateResponse from(Certificate c) {
        return new CertificateResponse(c.getId(), c.getName(), c.getIssuer(), c.getIssuedAt(), c.getExpiresAt(), c.getFileUrl(), c.getFileType());
    }
}
