package com.rikkei.jobs.company.dto.response;

import java.util.UUID;

/** Công ty trong chi tiết JD công khai: chỉ thông tin giới thiệu, không có mã số thuế hay trạng thái. */
public record PublicCompanyInfo(UUID id, String name, String logoUrl, String industry, String size, String city,
                                String website, String description) {
}
