package com.rikkei.jobs.identity.service.impl;

import com.rikkei.jobs.identity.config.AppProperties;
import com.rikkei.jobs.identity.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
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
}
