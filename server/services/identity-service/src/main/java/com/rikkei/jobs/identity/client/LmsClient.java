package com.rikkei.jobs.identity.client;

import com.rikkei.jobs.identity.dto.response.DevSsoTicketResponse;
import com.rikkei.jobs.identity.dto.response.LmsTicketInfo;

import java.util.Optional;

/** Gọi LMS (ở dev/demo là mock tại mock/, cổng 9000) kèm header X-Service-Key. */
public interface LmsClient {
    /** Đổi vé lấy danh tính; rỗng nếu LMS không nhận ra vé (404). Lỗi kết nối/khoá sai thì ném LMS_UNAVAILABLE. */
    Optional<LmsTicketInfo> verify(String ticket);

    /** Chỉ phục vụ dev/demo: nhờ LMS cấp vé cho một người dùng LMS. */
    DevSsoTicketResponse issueTicket(String lmsUserId);
}
