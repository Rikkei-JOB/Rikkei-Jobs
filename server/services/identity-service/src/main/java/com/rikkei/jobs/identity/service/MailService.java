package com.rikkei.jobs.identity.service;

public interface MailService {
    /** Không ném lỗi: quên mật khẩu phải trả 200 dù gửi email thất bại. */
    void sendPasswordReset(String to, String fullName, String link, long validMinutes);

    /**
     * Gửi mật khẩu tạm cho HR mới. Khác sendPasswordReset: gửi lỗi thì ném {@link org.springframework.mail.MailException}
     * để người gọi huỷ việc tạo user. Khi app.mail.enabled=false thì chỉ ghi log (không có mật khẩu).
     */
    void sendHrAccountCreated(String to, String fullName, String tempPassword, String loginUrl);
}
