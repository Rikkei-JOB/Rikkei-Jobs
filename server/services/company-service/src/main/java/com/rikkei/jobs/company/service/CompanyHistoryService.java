package com.rikkei.jobs.company.service;

import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.company.dto.response.CompanyActionItem;
import java.util.UUID;

public interface CompanyHistoryService {

    /** Lịch sử thao tác mới nhất trước; page từ 1, tham số sai ném BadRequestException, không có công ty thì 404. */
    PageResponse<CompanyActionItem> history(UUID companyId, int page, int size);
}
