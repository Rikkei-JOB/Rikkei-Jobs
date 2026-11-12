package com.rikkei.jobs.recruitment.service.impl;

import com.rikkei.jobs.recruitment.entity.Application;
import com.rikkei.jobs.recruitment.service.ApplicationMailSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.Component;

@Component
public class SmtpApplicationMailSender implements ApplicationMailSender {
    private final JavaMailSender sender;
    private final String from;
    public SmtpApplicationMailSender(JavaMailSender sender, @Value("${app.recruitment.mail-from}") String from) {
        this.sender = sender; this.from = from;
    }
    @Override public void sendNewApplication(UUID notificationId, String recipient, Application application) {
        try {
            var message = new MimeMessage(sender.createMimeMessage()) {
                @Override protected void updateMessageID() throws MessagingException {
                    setHeader("Message-ID", "<" + notificationId + "@rikkei-jobs.local>");
                }
            };
            var helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(from); helper.setTo(recipient);
            helper.setSubject("Có đơn ứng tuyển mới: " + application.getJobTitle());
            helper.setText("Công ty " + application.getCompanyName() + " nhận được đơn ứng tuyển mới.\n"
                + "Vị trí: " + application.getJobTitle() + "\nMã đơn: " + application.getId()
                + "\nVui lòng đăng nhập khu vực tuyển dụng để xem hồ sơ.");
            sender.send(message);
        } catch (MessagingException ex) { throw new MailPreparationException("Không thể chuẩn bị email ứng tuyển.", ex); }
    }
}
