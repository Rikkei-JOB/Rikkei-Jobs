package com.rikkei.jobs.company.client;

import java.util.UUID;

/** Gọi API nội bộ /internal/users của identity-service. */
public interface IdentityClient {

    /**
     * Tạo tài khoản HR. Email trùng thì ném {@link EmailExistsException}; lỗi mạng, timeout, 5xx, 401 thì ném
     * CompanyException(EXTERNAL_UNAVAILABLE); identity trả 400 thì ném BadRequestException.
     */
    CreatedUser createHrUser(String email, String fullName, UUID companyId);

    /** Xoá tài khoản HR (bù trừ khi ghi liên kết lỗi). 204 hoặc 404 coi là xong. */
    void deleteUser(UUID userId);
}
