package com.rikkei.jobs.company.service;

import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.company.dto.response.StaffCompanyDetail;
import com.rikkei.jobs.company.dto.response.StaffCompanyItem;
import com.rikkei.jobs.company.entity.CompanyStatus;
import java.util.UUID;

public interface StaffCompanyService {

    /** Tìm theo tên hoặc mã số thuế (không phân biệt hoa thường), page từ 1; tham số sai ném BadRequestException. */
    PageResponse<StaffCompanyItem> search(String q, CompanyStatus status, int page, int size);

    /** Chi tiết công ty kèm tài khoản HR (tra identity một lần); 404 nếu không có công ty. */
    StaffCompanyDetail detail(UUID id);
}
