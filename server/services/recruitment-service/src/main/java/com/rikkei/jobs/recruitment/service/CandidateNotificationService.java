package com.rikkei.jobs.recruitment.service;

import com.rikkei.jobs.common.config.exception.BadRequestException;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.recruitment.entity.Application;
import com.rikkei.jobs.recruitment.entity.ApplicationStatus;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service @RequiredArgsConstructor
public class CandidateNotificationService {
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public record Item(UUID id, String type, String title, String body, String href, Instant createdAt, Instant readAt) {}
    public record Feed(List<Item> items, long total, int page, int size, long unreadCount) {}

    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(UUID userId, UUID applicationId, String eventKey, String type, String title, String body, String href, Instant now) {
        jdbc.update("""
            insert into recruitment.candidate_notifications (id,user_id,application_id,event_key,type,title,body,href,created_at)
            values (?,?,?,?,?,?,?,?,?) on conflict(user_id,event_key) do nothing
            """, UUID.randomUUID(), userId, applicationId, eventKey, type, title, body, href, Timestamp.from(now));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void statusChanged(Application application, UUID historyId, ApplicationStatus target, Instant now) {
        String label = switch (target) {
            case SUBMITTED -> "Đã gửi"; case SCREENING -> "Sàng lọc"; case INTERVIEW -> "Phỏng vấn";
            case OFFER -> "Offer"; case HIRED -> "Nhận việc"; case REJECTED -> "Không đạt"; case WITHDRAWN -> "Đã rút";
        };
        String href = "/candidate/applications/" + application.getId();
        if (target == ApplicationStatus.OFFER) {
            var offers = jdbc.queryForList("select id from recruitment.offers where application_id=?", UUID.class, application.getId());
            if (!offers.isEmpty()) href = "/candidate/offers/" + offers.get(0);
        }
        publish(application.getUserId(), application.getId(), "status:" + historyId,
            target == ApplicationStatus.OFFER ? "OFFER" : "APPLICATION_STATUS",
            target == ApplicationStatus.OFFER ? "Bạn có offer mới" : "Cập nhật trạng thái ứng tuyển",
            application.getJobTitle() + " tại " + application.getCompanyName() + ": " + label + ".", href, now);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Feed list(UUID userId, int page, int size, boolean unreadOnly) {
        if (page < 1 || size < 1 || size > 100) throw new BadRequestException("page phải từ 1, size từ 1 đến 100.");
        var counts = jdbc.queryForMap("select count(*) as total, count(*) filter(where read_at is null) as unread from recruitment.candidate_notifications where user_id=?", userId);
        long unread = ((Number) counts.get("unread")).longValue();
        var items = jdbc.query("""
            select id,type,title,body,href,created_at,read_at from recruitment.candidate_notifications
            where user_id=? and (?=false or read_at is null)
            order by created_at desc,id desc limit ? offset ?
            """, (rs, row) -> new Item(rs.getObject("id", UUID.class), rs.getString("type"), rs.getString("title"),
                rs.getString("body"), rs.getString("href"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("read_at") == null ? null : rs.getTimestamp("read_at").toInstant()), userId, unreadOnly, size, (page - 1L) * size);
        return new Feed(items, unreadOnly ? unread : ((Number) counts.get("total")).longValue(), page, size, unread);
    }

    @Transactional
    public void read(UUID userId, UUID id) {
        int changed = jdbc.update("update recruitment.candidate_notifications set read_at=coalesce(read_at,?) where id=? and user_id=?", Timestamp.from(clock.instant()), id, userId);
        if (changed == 0) throw new ResourceNotFoundException("Không tìm thấy thông báo.");
    }

    @Transactional
    public void readAll(UUID userId) {
        jdbc.update("update recruitment.candidate_notifications set read_at=? where user_id=? and read_at is null", Timestamp.from(clock.instant()), userId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void remindInterviews(Instant now) {
        var timestamp = Timestamp.from(now);
        jdbc.update("""
            insert into recruitment.candidate_notifications (id,user_id,application_id,event_key,type,title,body,href,created_at)
            select gen_random_uuid(),s.candidate_id,a.id,'reminder:'||s.id||':'||
              case when t.start_at>?::timestamptz+interval '1 hour' then '24h' else '1h' end,
              'INTERVIEW_REMINDER',
              case when t.start_at>?::timestamptz+interval '1 hour' then 'Nhắc lịch phỏng vấn trong 24 giờ' else 'Nhắc lịch phỏng vấn trong 1 giờ' end,
              r.name||' — '||a.job_title||' tại '||a.company_name,'/candidate/interviews',?
            from recruitment.interview_schedules s join recruitment.interview_slots t on t.id=s.slot_id
              join recruitment.interview_rounds r on r.id=s.round_id join recruitment.applications a on a.id=r.application_id
            where s.status='SCHEDULED' and t.start_at>? and t.start_at<=?::timestamptz+interval '24 hours'
              and (t.start_at>?::timestamptz+interval '1 hour' or not s.reminder_1h_suppressed)
            on conflict(user_id,event_key) do nothing
            """, timestamp, timestamp, timestamp, timestamp, timestamp, timestamp);
    }
}
