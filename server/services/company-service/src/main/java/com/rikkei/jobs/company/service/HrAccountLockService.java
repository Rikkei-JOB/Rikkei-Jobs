package com.rikkei.jobs.company.service;

import com.rikkei.jobs.company.dto.response.StaffHrAccount;
import java.util.UUID;

public interface HrAccountLockService {

    /** Khoá HR ở identity rồi chép lockedAt về company_hr; userId không phải HR thì 404, không gọi identity. */
    StaffHrAccount lock(UUID userId, String reason);

    StaffHrAccount unlock(UUID userId);
}
