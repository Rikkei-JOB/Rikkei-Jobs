package com.rikkei.jobs.identity.service;

import com.rikkei.jobs.identity.dto.request.InternalCreateUserRequest;
import com.rikkei.jobs.identity.dto.response.InternalUserResponse;
import com.rikkei.jobs.identity.dto.response.InternalLmsIdentityResponse;

import java.util.UUID;

/** Nghiệp vụ cho API nội bộ mà company-service gọi để tạo/xoá tài khoản HR. */
public interface InternalUserService {

    InternalLmsIdentityResponse getLmsIdentity(UUID id);

    /** Tạo user HR với mật khẩu tạm gửi qua email; gửi mail lỗi thì không lưu user (MAIL_UNAVAILABLE). */
    InternalUserResponse createUser(InternalCreateUserRequest request);

    /** Xoá user HR (dùng để bù trừ khi company-service tạo hồ sơ thất bại). */
    void deleteHrUser(UUID id);
}
