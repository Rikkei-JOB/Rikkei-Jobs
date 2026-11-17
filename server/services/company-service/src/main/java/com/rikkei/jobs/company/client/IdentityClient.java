package com.rikkei.jobs.company.client;

import java.util.List;
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

    /**
     * Khoá tài khoản HR kèm lý do. 404 thì ném ResourceNotFoundException, 409 (không phải HR) thì ConflictException,
     * 400 thì BadRequestException; lỗi mạng, timeout, 5xx, 401 thì CompanyException(EXTERNAL_UNAVAILABLE).
     */
    IdentityUserStatus lockUser(UUID userId, String reason);

    /** Mở khoá tài khoản HR; lỗi map giống {@link #lockUser}. */
    IdentityUserStatus unlockUser(UUID userId);

    /** Tra trạng thái nhiều tài khoản (tối đa 100 id); id lạ bị bỏ qua. */
    List<IdentityUserStatus> lookupUsers(List<UUID> userIds);
}
