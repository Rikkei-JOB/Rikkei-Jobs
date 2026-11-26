package com.rikkei.jobs.company.dto.response;

import java.util.UUID;

/** Thông tin công ty đi kèm tin tuyển dụng công khai. */
public record PublicCompanySummary(UUID id, String name, String logoUrl, String industry, String size, String city) {
}
