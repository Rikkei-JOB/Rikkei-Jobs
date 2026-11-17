package com.rikkei.jobs.company.service.impl;

import com.rikkei.jobs.company.client.IdentityUserStatus;
import com.rikkei.jobs.company.dto.response.StaffHrAccount;

/** Dựng StaffHrAccount từ trạng thái identity trả về. */
final class StaffHrAccountMapping {

    private StaffHrAccountMapping() {
    }

    static StaffHrAccount from(IdentityUserStatus s) {
        return new StaffHrAccount(s.userId(), s.email(), s.fullName(), s.status(),
                s.lockedReason(), s.lockedAt(), s.tempLockedUntil());
    }
}
