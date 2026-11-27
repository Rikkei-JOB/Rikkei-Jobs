package com.rikkei.jobs.company.dto.response;

import java.util.UUID;

/** Một công ty trong GET /public/companies; openJobs là số tin đang hiển thị với ứng viên. */
public record PublicCompanyItem(UUID id, String name, String logoUrl, String industry, String size, String city,
                                long openJobs) {
}
