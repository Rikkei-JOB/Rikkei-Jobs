package com.rikkei.jobs.identity.service;

import com.rikkei.jobs.identity.dto.request.InternalCreateUserRequest;
import com.rikkei.jobs.identity.dto.response.InternalUserResponse;
import com.rikkei.jobs.identity.dto.response.InternalUserStatusResponse;
import com.rikkei.jobs.identity.dto.response.InternalLmsIdentityResponse;

import java.util.List;
import java.util.UUID;

/** Nghiệp vụ cho API nội bộ mà company-service gọi để tạo/xoá tài khoản HR. */
public interface InternalUserService {

    InternalLmsIdentityResponse getLmsIdentity(UUID id);

    InternalUserResponse getCandidate(UUID id);
    List<InternalUserResponse> getCandidates(List<UUID> ids);

    /** Tạo user HR với mật khẩu tạm gửi qua email; gửi mail lỗi thì không lưu user (MAIL_UNAVAILABLE). */
    InternalUserResponse createUser(InternalCreateUserRequest request);

    /** Xoá user HR (dùng để bù trừ khi company-service tạo hồ sơ thất bại). */
    void deleteHrUser(UUID id);

    /** Khoá tài khoản HR bởi cán bộ; đã khoá sẵn thì ghi đè lý do và thời điểm. */
    InternalUserStatusResponse lockHr(UUID id, String reason);

    /** Mở khoá cán bộ và xoá luôn khoá tạm; tài khoản không bị cán bộ khoá thì giữ nguyên. */
    InternalUserStatusResponse unlockHr(UUID id);

    /**
     * Đặt/bỏ cờ khoá theo công ty cho các HR trong danh sách; id không phải HR hoặc không tồn tại bị bỏ qua.
     * Khoá lại khi đã khoá giữ nguyên thời điểm cũ. Không đụng tới khoá riêng.
     */
    List<InternalUserStatusResponse> setCompanyLock(List<UUID> ids, boolean locked);

    /** Tra cứu trạng thái nhiều tài khoản theo thứ tự id đầu vào, bỏ id trùng và id không tồn tại. */
    List<InternalUserStatusResponse> lookup(List<UUID> ids);
}
