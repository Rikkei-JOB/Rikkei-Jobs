package com.rikkei.jobs.company.dto.response;

import java.util.UUID;

/** GET /public/companies/{id}: hồ sơ công khai, không có mã số thuế, trạng thái hay tài khoản HR. */
public record PublicCompanyDetail(UUID id, String name, String logoUrl, String industry, String size, String city,
                                  String address, String website, String description, long openJobs) {
}
