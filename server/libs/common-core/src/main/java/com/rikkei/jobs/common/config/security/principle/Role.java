package com.rikkei.jobs.common.config.security.principle;

public enum Role {
    CANDIDATE,
    HR,
    MENTOR,
    STAFF,
    ADMIN;

    /** Vai trò đăng nhập bằng vé SSO từ LMS (tất cả trừ HR doanh nghiệp). */
    public boolean isLmsRole() {
        return this != HR;
    }
}
