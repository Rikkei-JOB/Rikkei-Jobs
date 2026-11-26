package com.rikkei.jobs.company.service;

import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.company.dto.response.PublicJobDetail;
import com.rikkei.jobs.company.dto.response.PublicJobSummary;
import java.util.UUID;

/**
 * Trang việc làm công khai (DN-07 §4.3): chỉ JD hiện với ứng viên (APPROVED, còn hạn theo giờ Việt Nam,
 * công ty ACTIVE). Khách và người đã đăng nhập thấy như nhau.
 */
public interface PublicJobService {

    /** page từ 1, size 1–100, sai → 400; mới duyệt trước. */
    PageResponse<PublicJobSummary> list(int page, int size);

    /** JD không hiện với ứng viên (hoặc không tồn tại) → 404 như nhau. */
    PublicJobDetail get(UUID id);
}
