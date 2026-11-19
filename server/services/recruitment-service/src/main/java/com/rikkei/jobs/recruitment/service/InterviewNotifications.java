package com.rikkei.jobs.recruitment.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.recruitment.entity.*;
import com.rikkei.jobs.recruitment.repository.InterviewNotificationOutboxRepository;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
public class InterviewNotifications {
    private final InterviewNotificationOutboxRepository outbox;
    private final ObjectMapper json;
    private final JdbcTemplate jdbc;
    public void enqueue(String eventKey,String type,InterviewNotificationPayload payload,Collection<String> recipients,Instant now) {
        for(String email:recipients.stream().map(InterviewResourceLock::email).distinct().toList()) {
            var n=new InterviewNotificationOutbox();n.setId(UUID.randomUUID());n.setEventKey(eventKey);n.setType(type);
            n.setRoundId(payload.roundId());n.setScheduleId(payload.scheduleId());n.setRecipient(email);
            try {n.setPayload(json.writeValueAsString(payload));}catch(Exception ex){throw new IllegalStateException(ex);}
            n.setStatus("PENDING");n.setCreatedAt(now);n.setNextAttemptAt(now);outbox.save(n);
        }
    }
    public void cancelPending(UUID roundId) {
        jdbc.update("update recruitment.interview_notification_outbox set status='CANCELLED' where round_id=? and status in ('PENDING','PROCESSING','FAILED') and type<>'CANCELLATION'",roundId);
    }
    public List<String> attemptedRecipients(UUID scheduleId) {
        return jdbc.queryForList("select recipient from recruitment.interview_notification_outbox where schedule_id=? and type='CONFIRMATION' and (status='SENT' or attempts>0)",String.class,scheduleId);
    }
}
