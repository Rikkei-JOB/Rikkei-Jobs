package com.rikkei.jobs.company.service;

import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.company.dto.response.PublicCompanyDetail;
import com.rikkei.jobs.company.dto.response.PublicCompanyItem;
import com.rikkei.jobs.company.dto.response.PublicJobDetail;
import com.rikkei.jobs.company.dto.response.PublicJobItem;
import com.rikkei.jobs.company.entity.JobLevel;
import com.rikkei.jobs.company.entity.JobType;
import com.rikkei.jobs.company.entity.WorkMode;
import java.util.List;
import java.util.UUID;

/** Bảng việc làm công khai (không cần đăng nhập): chỉ tin APPROVED, còn hạn, của công ty ACTIVE. */
public interface PublicJobService {

    /** Bộ lọc tìm tin; trường null là không lọc. sort: "newest" (mặc định) hoặc "salary". */
    record JobQuery(String q, List<UUID> skillIds, String city, JobLevel level, WorkMode workMode, JobType type,
                    Long salaryFrom, Long salaryTo, UUID companyId, String sort, int page, int size) {
    }

    PageResponse<PublicJobItem> search(JobQuery query);

    PublicJobDetail get(UUID jobId);

    List<PublicCompanyItem> companies();

    PublicCompanyDetail company(UUID companyId);
}
