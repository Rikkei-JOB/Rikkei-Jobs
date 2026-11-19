package com.rikkei.jobs.recruitment;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.recruitment.service.*;
import com.rikkei.jobs.recruitment.service.impl.SmtpInterviewMailSender;
import com.rikkei.jobs.recruitment.entity.InterviewNotificationOutbox;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class SmtpInterviewMailSenderTest {
    @Test void producesVietnameseMimeWithCalendarAndStableMessageId() throws Exception {
        var sender=mock(JavaMailSender.class);when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        var json=new ObjectMapper().findAndRegisterModules();var calendar=new InterviewCalendarWriter(json);
        var n=new InterviewNotificationOutbox();n.setId(UUID.randomUUID());n.setType("CONFIRMATION");n.setRecipient("candidate@test.local");n.setCreatedAt(Instant.EPOCH);
        n.setPayload(json.writeValueAsString(new InterviewNotificationPayload(UUID.randomUUID(),UUID.randomUUID(),"Kỹ thuật","Công ty A","Ứng viên","candidate@test.local","hr@test.local",Instant.parse("2030-01-01T09:00:00Z"),Instant.parse("2030-01-01T09:30:00Z"),"https://meet.test/room")));
        var captured=org.mockito.ArgumentCaptor.forClass(MimeMessage.class);
        new SmtpInterviewMailSender(sender,calendar,"noreply@test.local","https://jobs.test").send(n);verify(sender).send(captured.capture());
        var msg=captured.getValue();msg.saveChanges();assertEquals("<"+n.getId()+"@rikkei-jobs.local>",msg.getMessageID());assertTrue(msg.getSubject().contains("Xác nhận"));
        var mixed=(Multipart)msg.getContent();assertTrue(mixed.getCount()>=2);var attachment=mixed.getBodyPart(1);assertEquals("interview.ics",attachment.getFileName());assertTrue(attachment.getContentType().toLowerCase().contains("method=request"));
        String ics=new String(attachment.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);assertTrue(ics.contains("ATTENDEE;ROLE=REQ-PARTICIPANT:mailto:candidate@test.local"));assertTrue(ics.contains("ATTENDEE;ROLE=REQ-PARTICIPANT:mailto:hr@test.local"));
    }
}
