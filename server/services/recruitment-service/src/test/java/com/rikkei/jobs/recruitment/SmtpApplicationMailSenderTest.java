package com.rikkei.jobs.recruitment;

import com.rikkei.jobs.recruitment.entity.Application;
import com.rikkei.jobs.recruitment.service.impl.SmtpApplicationMailSender;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SmtpApplicationMailSenderTest {
    @Test void mailPreservesVietnameseAndStableMessageIdWithoutAttachingPrivateCv() throws Exception {
        var sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        var application = new Application();
        application.setId(UUID.randomUUID()); application.setJobTitle("Kỹ sư phần mềm"); application.setCompanyName("Công ty A");
        var notification = UUID.randomUUID();
        new SmtpApplicationMailSender(sender, "no-reply@example.test").sendNewApplication(notification, "hr@example.test", application);
        var sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(sent.capture());
        var message = sent.getValue();
        message.saveChanges(); message.saveChanges();
        assertThat(message.getMessageID()).isEqualTo("<" + notification + "@rikkei-jobs.local>");
        assertThat(message.getSubject()).contains("Kỹ sư phần mềm");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("hr@example.test");
        assertThat(message.getContent()).asString().contains("Công ty A").contains(application.getId().toString());
        assertThat(message.getContentType()).startsWith("text/plain");
    }
}
