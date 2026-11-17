package com.rikkei.jobs.recruitment.service.impl;

import com.rikkei.jobs.recruitment.entity.Application;
import com.rikkei.jobs.recruitment.entity.ApplicationStatusHistory;
import com.rikkei.jobs.recruitment.service.ApplicationStatusMailSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class SmtpApplicationStatusMailSender implements ApplicationStatusMailSender {
    private final JavaMailSender sender;
    private final String from;

    public SmtpApplicationStatusMailSender(JavaMailSender sender, @Value("${app.recruitment.mail-from}") String from) {
        this.sender = sender;
        this.from = from;
    }

    @Override
    public void send(UUID notificationId, String email, ApplicationStatusHistory history, Application application) {
        try {
            var message = new MimeMessage(sender.createMimeMessage().getSession()) {
                @Override protected void updateMessageID() throws MessagingException {
                    setHeader("Message-ID", "<" + notificationId + "@rikkei-jobs.local>");
                }
            };
            var helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(email);
            helper.setSubject("Cập nhật đơn ứng tuyển: " + application.getJobTitle());
            helper.setText("Đơn ứng tuyển vị trí " + application.getJobTitle()
                + " của bạn đã chuyển sang trạng thái " + history.getToStatus()
                + ".\nMã đơn: " + application.getId()
                + "\nVui lòng đăng nhập để xem chi tiết.");
            sender.send(message);
        } catch (MessagingException ex) {
            throw new MailPreparationException("Không thể chuẩn bị email trạng thái đơn.", ex);
        }
    }
}
