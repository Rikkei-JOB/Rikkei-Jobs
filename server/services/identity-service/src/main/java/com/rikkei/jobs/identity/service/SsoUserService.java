package com.rikkei.jobs.identity.service;

import com.rikkei.jobs.identity.dto.response.LmsTicketInfo;
import com.rikkei.jobs.identity.entity.User;

public interface SsoUserService {
    /** Tìm người dùng theo vé đã xác thực, chưa có thì tạo với vai trò LMS trả về. */
    User findOrCreate(LmsTicketInfo info);
}
