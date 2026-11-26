package com.rikkei.jobs.company.dto.response;

import java.util.UUID;

/** Tham chiếu gọn tới công ty trong các danh sách của cán bộ. */
public record CompanyRef(UUID id, String name) {
}
