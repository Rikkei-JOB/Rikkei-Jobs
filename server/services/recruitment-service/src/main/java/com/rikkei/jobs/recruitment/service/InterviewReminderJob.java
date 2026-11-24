package com.rikkei.jobs.recruitment.service;
import java.sql.Timestamp;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class InterviewReminderJob {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final CandidateNotificationService candidateNotifications;
    @Value("${app.recruitment.interview-jobs-enabled:true}") private boolean enabled;
    @org.springframework.transaction.annotation.Transactional
    @Scheduled(cron="0 */15 * * * *") public void scheduled(){if(enabled)enqueueDue();}
    @org.springframework.transaction.annotation.Transactional
    public int enqueueDue() {
        candidateNotifications.remindInterviews(clock.instant());
        var now=Timestamp.from(clock.instant());
        return jdbc.update("""
            insert into recruitment.interview_notification_outbox
            (id,event_key,type,round_id,schedule_id,recipient,payload,status,attempts,next_attempt_at,created_at)
            select gen_random_uuid(),'schedule:'||s.id,
              case when t.start_at>?::timestamptz+interval '1 hour' then 'REMINDER_24H' else 'REMINDER_1H' end,
              s.round_id,s.id,n.recipient,n.payload,'PENDING',0,?,?
            from recruitment.interview_schedules s join recruitment.interview_slots t on t.id=s.slot_id
            join recruitment.interview_notification_outbox n on n.schedule_id=s.id and n.type='CONFIRMATION'
            where s.status='SCHEDULED' and t.start_at>? and t.start_at<=?::timestamptz+interval '24 hours'
              and (t.start_at>?::timestamptz+interval '1 hour' or not s.reminder_1h_suppressed)
            on conflict(event_key,type,recipient) do nothing
            """,now,now,now,now,now,now);
    }
}
