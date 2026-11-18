package com.rikkei.jobs.recruitment;

import com.rikkei.jobs.recruitment.entity.Application;
import com.rikkei.jobs.recruitment.entity.ApplicationStatus;
import com.rikkei.jobs.recruitment.entity.ApplicationStatusHistory;
import com.rikkei.jobs.recruitment.entity.Offer;
import com.rikkei.jobs.recruitment.entity.OfferStatus;
import com.rikkei.jobs.recruitment.repository.OfferRepository;
import com.rikkei.jobs.recruitment.service.impl.SmtpApplicationStatusMailSender;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SmtpApplicationStatusMailSenderTest {
    @Test void candidateMailHasCurrentStatusAndStableIdWithoutInternalNote() throws Exception {
        var sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        var application = new Application();
        application.setId(UUID.randomUUID());
        application.setJobTitle("Kỹ sư phần mềm");
        var history = new ApplicationStatusHistory();
        history.setToStatus(ApplicationStatus.INTERVIEW);
        history.setNote("Ghi chú HR tuyệt mật");
        var notification = UUID.randomUUID();
        new SmtpApplicationStatusMailSender(sender, mock(OfferRepository.class),
            "no-reply@example.test", "http://localhost:3000")
            .send(notification, "candidate@example.test", history, application);
        var sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(sent.capture());
        var message = sent.getValue();
        message.saveChanges(); message.saveChanges();
        assertThat(message.getMessageID()).isEqualTo("<" + notification + "@rikkei-jobs.local>");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("candidate@example.test");
        assertThat(message.getSubject()).contains("Kỹ sư phần mềm");
        assertThat(message.getContent().toString()).contains("INTERVIEW").contains(application.getId().toString())
            .doesNotContain("Ghi chú HR tuyệt mật");
    }

    @Test void offerTransitionEmailContainsOfferTermsAndOneMessage() throws Exception {
        var sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        var offers = mock(OfferRepository.class);
        var application = new Application();
        application.setId(UUID.randomUUID()); application.setJobTitle("Java Developer");
        var offer = new Offer();
        offer.setId(UUID.randomUUID()); offer.setApplicationId(application.getId());
        offer.setPosition("Java Developer"); offer.setSalary(12000000L);
        offer.setStartDate(LocalDate.parse("2030-02-01")); offer.setWorkLocation("Hà Nội");
        offer.setResponseDeadline(Instant.parse("2030-01-03T00:00:00Z")); offer.setStatus(OfferStatus.SENT);
        when(offers.findByApplicationId(application.getId())).thenReturn(Optional.of(offer));
        var history = new ApplicationStatusHistory(); history.setToStatus(ApplicationStatus.OFFER);
        new SmtpApplicationStatusMailSender(sender, offers, "no-reply@example.test", "http://localhost:3000")
            .send(UUID.randomUUID(), "candidate@example.test", history, application);
        var sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender, times(1)).send(sent.capture());
        String body = sent.getValue().getContent().toString();
        assertThat(body).contains("12.000.000", "gross", "2030-02-01", "Hà Nội",
            "2030-01-03", "http://localhost:3000/candidate/offers/" + offer.getId());
    }
}
