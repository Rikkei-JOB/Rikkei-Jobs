package com.rikkei.jobs.recruitment.service;
import com.rikkei.jobs.recruitment.entity.InterviewNotificationOutbox;
import com.rikkei.jobs.recruitment.repository.InterviewNotificationOutboxRepository;
import java.sql.*;
import java.time.*;
import java.util.UUID;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component @Slf4j
public class InterviewNotificationWorker {
    private final JdbcTemplate jdbc;
    private final DataSource dataSource;
    private final TransactionTemplate tx;
    private final InterviewNotificationOutboxRepository outbox;
    private final InterviewMailSender sender;
    private final Clock clock;
    private final boolean enabled;
    public InterviewNotificationWorker(JdbcTemplate jdbc,DataSource dataSource,PlatformTransactionManager manager,InterviewNotificationOutboxRepository outbox,InterviewMailSender sender,Clock clock,@Value("${app.recruitment.interview-jobs-enabled:true}") boolean enabled){this.jdbc=jdbc;this.dataSource=dataSource;this.tx=new TransactionTemplate(manager);this.outbox=outbox;this.sender=sender;this.clock=clock;this.enabled=enabled;}
    @Scheduled(fixedDelayString="${app.recruitment.interview-outbox-delay-ms:10000}") public void scheduled(){if(enabled)dispatchBatch();}
    public int dispatchBatch(){int processed=0;for(int i=0;i<20;i++){var n=claim();if(n==null)break;deliver(n);processed++;}return processed;}
    InterviewNotificationOutbox claim(){
        UUID id=tx.execute(status->{
            var now=Timestamp.from(clock.instant());
            var rows=jdbc.queryForList("""
                select id,schedule_id,round_id,attempts from recruitment.interview_notification_outbox
                where (status='PENDING' and next_attempt_at<=?) or (status='PROCESSING' and lease_until<=?)
                order by created_at,id limit 20 for update skip locked
                """,now,now);
            for(var row:rows) {
                String key=sendKey((UUID)row.get("schedule_id"),(UUID)row.get("round_id"));
                // A lease expiring does not mean the SMTP operation has stopped.
                if(!Boolean.TRUE.equals(jdbc.queryForObject("select pg_try_advisory_xact_lock(hashtextextended(?,6007))",Boolean.class,key)))continue;
                UUID found=(UUID)row.get("id");
                if(((Number)row.get("attempts")).intValue()>=5){jdbc.update("update recruitment.interview_notification_outbox set status='FAILED',lease_until=null,claim_token=null where id=?",found);continue;}
                jdbc.update("update recruitment.interview_notification_outbox set status='PROCESSING',attempts=attempts+1,lease_until=?,claim_token=? where id=?",Timestamp.from(clock.instant().plusSeconds(60)),UUID.randomUUID(),found);
                return found;
            }
            return null;
        });
        return id==null?null:outbox.findById(id).orElse(null);
    }
    void deliver(InterviewNotificationOutbox n){
        String key=sendKey(n.getScheduleId(),n.getRoundId());
        try(Connection connection=dataSource.getConnection()) {
            boolean locked;
            try(var stmt=connection.prepareStatement("select pg_try_advisory_lock(hashtextextended(?,6007))")){stmt.setString(1,key);try(var rs=stmt.executeQuery()){rs.next();locked=rs.getBoolean(1);}}
            if(!locked){jdbc.update("update recruitment.interview_notification_outbox set status='PENDING',attempts=attempts-1,next_attempt_at=?,lease_until=null,claim_token=null where id=? and status='PROCESSING' and claim_token=?",Timestamp.from(clock.instant().plusSeconds(10)),n.getId(),n.getClaimToken());return;}
            try {
                if(!eligible(n)) {jdbc.update("update recruitment.interview_notification_outbox set status='CANCELLED',lease_until=null,claim_token=null where id=? and status='PROCESSING' and claim_token=?",n.getId(),n.getClaimToken());return;}
                sender.send(n);
                tx.executeWithoutResult(status->{
                    int changed=jdbc.update("update recruitment.interview_notification_outbox set status='SENT',sent_at=?,lease_until=null,claim_token=null where id=? and status='PROCESSING' and claim_token=?",Timestamp.from(clock.instant()),n.getId(),n.getClaimToken());
                    if(changed==1)milestone(n);
                });
            } finally {try(var stmt=connection.prepareStatement("select pg_advisory_unlock(hashtextextended(?,6007))")){stmt.setString(1,key);stmt.execute();}}
        }catch(Exception ex){
            int[] delays={60,300,900,3600};boolean failed=n.getAttempts()>=5;
            jdbc.update("update recruitment.interview_notification_outbox set status=?,next_attempt_at=?,lease_until=null,claim_token=null where id=? and status='PROCESSING' and claim_token=?",failed?"FAILED":"PENDING",Timestamp.from(clock.instant().plusSeconds(failed?0:delays[n.getAttempts()-1])),n.getId(),n.getClaimToken());
            log.warn("Interview notification {} attempt {} failed ({})",n.getId(),n.getAttempts(),ex.getClass().getSimpleName());
        }
    }
    boolean eligible(InterviewNotificationOutbox n){
        if(!Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from recruitment.interview_notification_outbox where id=? and status='PROCESSING' and claim_token=?)",Boolean.class,n.getId(),n.getClaimToken())))return false;
        if("CANCELLATION".equals(n.getType()))return true;
        var now=Timestamp.from(clock.instant());
        if("INVITATION".equals(n.getType()))return Boolean.TRUE.equals(jdbc.queryForObject("""
            select exists(select 1 from recruitment.interview_rounds r join recruitment.applications a on a.id=r.application_id
            join recruitment.interview_slots t on t.round_id=r.id where r.id=? and a.status='INTERVIEW' and r.result='PENDING' and t.status='OPEN' and t.start_at>?)
            """,Boolean.class,n.getRoundId(),now));
        var states=jdbc.queryForList("select s.status,t.start_at,s.reminder_1h_suppressed from recruitment.interview_schedules s join recruitment.interview_slots t on t.id=s.slot_id where s.id=?",n.getScheduleId());
        if(states.isEmpty())return false;var s=states.get(0);
        Object value=s.get("start_at");
        Instant start=value instanceof OffsetDateTime time?time.toInstant():((Timestamp)value).toInstant();
        if(!"SCHEDULED".equals(s.get("status")) || !start.isAfter(clock.instant()))return false;
        if("REMINDER_24H".equals(n.getType()))return start.isAfter(clock.instant().plusSeconds(3600)) && !start.isAfter(clock.instant().plusSeconds(86400));
        if("REMINDER_1H".equals(n.getType()))return !start.isAfter(clock.instant().plusSeconds(3600)) && !Boolean.TRUE.equals(s.get("reminder_1h_suppressed"));
        return true;
    }
    void milestone(InterviewNotificationOutbox n){
        String column=switch(n.getType()){case "REMINDER_24H"->"reminded_24h_at";case "REMINDER_1H"->"reminded_1h_at";default->null;};
        if(column==null)return;
        jdbc.update("update recruitment.interview_schedules s set "+column+"=? where s.id=? and s.status='SCHEDULED' and not exists(select 1 from recruitment.interview_notification_outbox n where n.schedule_id=s.id and n.type=? and n.status<>'SENT')",Timestamp.from(clock.instant()),n.getScheduleId(),n.getType());
    }
    static String sendKey(UUID scheduleId,UUID roundId){return scheduleId==null?"proposal:"+roundId:"schedule:"+scheduleId;}
}
