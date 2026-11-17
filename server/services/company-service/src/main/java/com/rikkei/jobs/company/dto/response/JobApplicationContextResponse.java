package com.rikkei.jobs.company.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Ngữ cảnh JD cho UT-01 (nộp hồ sơ); tên field khớp record JobApplicationContext của recruitment-service. */
public record JobApplicationContextResponse(UUID jobId, UUID companyId, String jobTitle, String companyName,
                                            String status, OffsetDateTime deadline, boolean companyLocked,
                                            List<String> hrEmails) {
}
