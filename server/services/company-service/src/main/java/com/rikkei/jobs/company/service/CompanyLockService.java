package com.rikkei.jobs.company.service;

import com.rikkei.jobs.company.dto.response.StaffCompanyDetail;
import java.util.UUID;

/**
 * Cán bộ khoá / mở khoá công ty (DN-07 §4.5): đổi companies.status, ghi company_actions và bật / tắt cờ khoá theo
 * công ty của mọi HR ở identity-service, tất cả hoặc không gì cả.
 */
public interface CompanyLockService {

    /** ACTIVE → LOCKED kèm lý do (strip, 1–1000); không có công ty → 404; đã khoá → 409; identity lỗi → 503. */
    StaffCompanyDetail lock(UUID staffId, UUID companyId, String reason);

    /** LOCKED → ACTIVE; không có công ty → 404; đang hoạt động → 409; identity lỗi → 503. */
    StaffCompanyDetail unlock(UUID staffId, UUID companyId);
}
