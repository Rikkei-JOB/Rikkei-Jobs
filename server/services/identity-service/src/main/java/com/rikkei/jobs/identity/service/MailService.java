package com.rikkei.jobs.identity.service;

public interface MailService {
    /** Không ném lỗi: quên mật khẩu phải trả 200 dù gửi email thất bại. */
    void sendPasswordReset(String to, String fullName, String link, long validMinutes);
}
