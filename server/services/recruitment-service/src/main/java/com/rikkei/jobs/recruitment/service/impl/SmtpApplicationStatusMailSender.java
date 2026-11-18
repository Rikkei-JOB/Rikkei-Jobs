package com.rikkei.jobs.recruitment.service.impl;

import com.rikkei.jobs.recruitment.entity.Application;
import com.rikkei.jobs.recruitment.entity.ApplicationStatusHistory;
import com.rikkei.jobs.recruitment.entity.ApplicationStatus;
import com.rikkei.jobs.recruitment.repository.OfferRepository;
import com.rikkei.jobs.recruitment.service.ApplicationStatusMailSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.text.NumberFormat;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class SmtpApplicationStatusMailSender implements ApplicationStatusMailSender {
    private final JavaMailSender sender;
    private final OfferRepository offers;
    private final String from;
    private final String webBaseUrl;

    public SmtpApplicationStatusMailSender(JavaMailSender sender, OfferRepository offers,
        @Value("${app.recruitment.mail-from}") String from,
        @Value("${app.recruitment.web-base-url:http://localhost:3000}") String webBaseUrl) {
        this.sender = sender;
        this.offers = offers;
        this.from = from;
        this.webBaseUrl = webBaseUrl.replaceAll("/+$", "");
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
            var offer = history.getToStatus() == ApplicationStatus.OFFER
                ? offers.findByApplicationId(application.getId()) : java.util.Optional.<com.rikkei.jobs.recruitment.entity.Offer>empty();
            if (offer.isPresent()) {
                var terms = offer.get();
                String salary = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(terms.getSalary());
                helper.setSubject("Offer làm việc: " + terms.getPosition());
                helper.setText("Bạn nhận được offer cho vị trí " + terms.getPosition()
                    + ".\nLương: " + salary + " VND/tháng (gross)"
                    + "\nNgày bắt đầu: " + terms.getStartDate()
                    + "\nĐịa điểm: " + terms.getWorkLocation()
                    + "\nHạn trả lời: " + terms.getResponseDeadline()
                    + (terms.getNote() == null || terms.getNote().isBlank() ? "" : "\nGhi chú: " + terms.getNote())
                    + "\nXem và phản hồi: " + webBaseUrl + "/candidate/offers/" + terms.getId());
            } else {
                helper.setSubject("Cập nhật đơn ứng tuyển: " + application.getJobTitle());
                helper.setText("Đơn ứng tuyển vị trí " + application.getJobTitle()
                    + " của bạn đã chuyển sang trạng thái " + history.getToStatus()
                    + ".\nMã đơn: " + application.getId()
                    + "\nVui lòng đăng nhập để xem chi tiết.");
            }
            sender.send(message);
        } catch (MessagingException ex) {
            throw new MailPreparationException("Không thể chuẩn bị email trạng thái đơn.", ex);
        }
    }
}
