package com.rikkei.jobs.company.dto.response;

import java.util.UUID;

/** Công ty gắn trong một dòng của trang việc làm công khai. */
public record PublicCompanyRef(UUID id, String name, String logoUrl) {
}
