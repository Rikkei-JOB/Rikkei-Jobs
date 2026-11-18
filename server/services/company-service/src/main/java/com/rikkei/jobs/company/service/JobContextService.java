package com.rikkei.jobs.company.service;

import com.rikkei.jobs.company.dto.response.JobApplicationContextResponse;
import java.util.UUID;

/** Dữ liệu JD cho các service khác gọi nội bộ (UT-01). */
public interface JobContextService {

    /** JD không tồn tại thì ném ResourceNotFoundException; identity lỗi thì CompanyException(EXTERNAL_UNAVAILABLE). */
    JobApplicationContextResponse applicationContext(UUID jobId);
}
