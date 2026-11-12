package com.rikkei.jobs.identity.service.impl;

import com.rikkei.jobs.identity.config.AppProperties;
import com.rikkei.jobs.identity.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final AppProperties properties;

    @Override
    public void sendPasswordReset(String to, String fullName, String link, long validMinutes) {
        if (!properties.getMail().isEnabled()) {
            // Dev không có SMTP: in link ra log thay vì gửi email.
            log.info("[mail disabled] Link đặt lại mật khẩu cho {}: {}", to, link);
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Chưa cấu hình JavaMailSender, không gửi được email đặt lại mật khẩu tới {}", to);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.getMail().getFrom());
            message.setTo(to);
            message.setSubject("Đặt lại mật khẩu Rikkei Jobs");
            message.setText("Xin chào " + fullName + ",\n\n"
                    + "Bạn vừa yêu cầu đặt lại mật khẩu. Mở liên kết sau để đặt mật khẩu mới "
                    + "(hiệu lực " + validMinutes + " phút, chỉ dùng một lần):\n\n"
                    + link + "\n\n"
                    + "Nếu bạn không yêu cầu, hãy bỏ qua email này.");
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Không gửi được email đặt lại mật khẩu tới {}: {}", to, e.getMessage());
        }
    }

    @Override
    public void sendHrAccountCreated(String to, String fullName, String tempPassword, String loginUrl) {
        if (!properties.getMail().isEnabled()) {
            // Không ghi mật khẩu tạm ra log.
            log.info("[mail disabled] Bỏ qua email tài khoản HR cho {}", to);
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            // Mail bật mà không có sender thì coi là gửi lỗi, để không tạo user mà HR không nhận được mật khẩu.
            throw new MailSendException("Chưa cấu hình JavaMailSender");
        }
        // Không bắt MailException: để InternalUserService quyết định (huỷ việc tạo user).
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getMail().getFrom());
        message.setTo(to);
        message.setSubject("Tài khoản HR Rikkei Jobs của bạn");
        message.setText("Xin chào " + fullName + ",\n\n"
                + "Tài khoản HR Rikkei Jobs của bạn đã được tạo.\n\n"
                + "Email đăng nhập: " + to + "\n"
                + "Mật khẩu tạm: " + tempPassword + "\n"
                + "Đăng nhập tại: " + loginUrl + "\n\n"
                + "Bạn sẽ phải đổi mật khẩu ở lần đăng nhập đầu tiên.");
        mailSender.send(message);
    }
}
