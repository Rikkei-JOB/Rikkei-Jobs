package com.rikkei.jobs.company.service.impl;

import com.rikkei.jobs.company.client.IdentityUserStatus;
import com.rikkei.jobs.company.dto.response.StaffHrAccount;

/** Dựng StaffHrAccount từ trạng thái identity trả về. */
final class StaffHrAccountMapping {

    private StaffHrAccountMapping() {
    }

    /**
     * Không chép thẳng {@code status} của identity: ở đó LOCKED gồm cả khoá theo công ty, còn StaffHrAccount.status
     * chỉ nói về khoá riêng (để nút Khoá / Mở khoá riêng vẫn đúng khi công ty đang bị khoá).
     */
    static StaffHrAccount from(IdentityUserStatus s) {
        return new StaffHrAccount(s.userId(), s.email(), s.fullName(), s.lockedAt() != null ? "LOCKED" : "ACTIVE",
                s.lockedReason(), s.lockedAt(), s.tempLockedUntil(), Boolean.TRUE.equals(s.companyLocked()));
    }
}
