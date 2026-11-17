package com.rikkei.jobs.recruitment;

import com.rikkei.jobs.recruitment.entity.Application;
import com.rikkei.jobs.recruitment.entity.ApplicationStatus;
import com.rikkei.jobs.recruitment.entity.ApplicationStatusHistory;
import com.rikkei.jobs.recruitment.service.impl.SmtpApplicationStatusMailSender;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.UUID;
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
        new SmtpApplicationStatusMailSender(sender, "no-reply@example.test")
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
}
