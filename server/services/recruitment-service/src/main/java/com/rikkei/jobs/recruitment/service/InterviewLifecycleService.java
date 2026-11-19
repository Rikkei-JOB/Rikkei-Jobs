package com.rikkei.jobs.recruitment.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.recruitment.repository.*;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service @RequiredArgsConstructor
public class InterviewLifecycleService {
    private final InterviewRoundRepository rounds;
    private final InterviewScheduleRepository schedules;
    private final InterviewSlotRepository slots;
    private final InterviewNotifications notifications;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    @Transactional(propagation=Propagation.MANDATORY)
    public void cancelForApplication(UUID applicationId,Instant now) {
        for(var round:rounds.findByApplicationIdOrderByRoundNoAsc(applicationId)) cancelRound(round.getId(),now);
    }
    @Transactional(propagation=Propagation.MANDATORY)
    public void cancelRound(UUID roundId,Instant now) {
        // Lock/cancel rows before reading attempts. Concurrent claims SKIP LOCKED,
        // and any earlier claim is visible when this update finishes.
        notifications.cancelPending(roundId);
        var cancelledSlots=new HashSet<UUID>();
        for(var schedule:schedules.findByRoundIdOrderByCreatedAtDesc(roundId)) {
            if(!"SCHEDULED".equals(schedule.getStatus())) continue;
            cancelledSlots.add(schedule.getSlotId());
            var recipients=notifications.attemptedRecipients(schedule.getId());
            var payloads=jdbc.queryForList("select payload from recruitment.interview_notification_outbox where schedule_id=? and type='CONFIRMATION' limit 1",String.class,schedule.getId());
            schedule.setStatus("CANCELLED");schedule.setCancelledAt(now);schedules.save(schedule);
            if(!recipients.isEmpty() && !payloads.isEmpty()) {
                try {notifications.enqueue("schedule:"+schedule.getId(),"CANCELLATION",json.readValue(payloads.get(0),InterviewNotificationPayload.class),recipients,now);}
                catch(com.fasterxml.jackson.core.JsonProcessingException ex){throw new IllegalStateException(ex);}
            }
        }
        for(var slot:slots.findByRoundIdOrderByStartAtAsc(roundId)) if("OPEN".equals(slot.getStatus()) || cancelledSlots.contains(slot.getId())){slot.setStatus("CANCELLED");slots.save(slot);}
        if(!cancelledSlots.isEmpty()) rounds.findById(roundId).ifPresent(r->{r.setScheduledAt(null);rounds.save(r);});
    }
    @Transactional(propagation=Propagation.MANDATORY)
    public void completeForRound(UUID roundId,Instant now) {
        notifications.cancelPending(roundId);
        schedules.findByRoundIdAndStatus(roundId,"SCHEDULED").ifPresent(s->{s.setStatus("DONE");schedules.save(s);});
        for(var slot:slots.findByRoundIdOrderByStartAtAsc(roundId)) if("OPEN".equals(slot.getStatus())){slot.setStatus("CANCELLED");slots.save(slot);}
    }
}
