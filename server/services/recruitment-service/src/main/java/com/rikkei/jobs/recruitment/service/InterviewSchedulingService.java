package com.rikkei.jobs.recruitment.service;

import com.rikkei.jobs.common.config.exception.*;
import com.rikkei.jobs.common.config.security.principle.*;
import com.rikkei.jobs.recruitment.client.*;
import com.rikkei.jobs.recruitment.dto.request.*;
import com.rikkei.jobs.recruitment.dto.response.*;
import com.rikkei.jobs.recruitment.entity.*;
import com.rikkei.jobs.recruitment.repository.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class InterviewSchedulingService {
    private final ApplicationRepository applications;
    private final InterviewRoundRepository rounds;
    private final InterviewSlotRepository slots;
    private final InterviewScheduleRepository schedules;
    private final InterviewResourceLock resources;
    private final IdentityCandidateClient identities;
    private final MeetRoomClient meet;
    private final InterviewNotifications notifications;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final InterviewLifecycleService lifecycle;

    record Context(Application application,InterviewRound round) {}
    Context locked(AuthPrincipal actor,UUID roundId,boolean hr) {
        if(actor==null || actor.getRole()!=(hr?Role.HR:Role.CANDIDATE)) throw new ForbiddenException("Không có quyền đặt lịch.");
        UUID appId=rounds.findApplicationIdByRoundId(roundId).orElseThrow(InterviewSchedulingService::missing);
        var app=applications.findForUpdate(appId).orElseThrow(InterviewSchedulingService::missing);
        if(hr ? !Objects.equals(app.getCompanyId(),actor.getCompanyId()) : !Objects.equals(app.getUserId(),actor.getUserId())) throw missing();
        var round=rounds.findForUpdate(roundId).orElseThrow(InterviewSchedulingService::missing);
        if(app.getStatus()!=ApplicationStatus.INTERVIEW || round.getResult()!=InterviewRoundResult.PENDING) throw new ConflictException("Vòng phỏng vấn không còn cho phép đặt lịch.");
        return new Context(app,round);
    }
    static ResourceNotFoundException missing(){return new ResourceNotFoundException("Không tìm thấy lịch phỏng vấn.");}
    void validate(List<InterviewSlotRequest> requests) {
        if(requests==null || requests.isEmpty() || requests.size()>5) throw new BadRequestException("Cần đề xuất từ 1 đến 5 khung giờ.");
        var sorted=new ArrayList<>(requests);
        for(var r:sorted) if(r==null || r.startAt()==null || r.endAt()==null || !r.startAt().toInstant().isAfter(clock.instant()) || Duration.between(r.startAt().toInstant(),r.endAt().toInstant()).compareTo(Duration.ofMinutes(30))<0)
            throw new BadRequestException("Khung giờ phải ở tương lai và dài ít nhất 30 phút.");
        sorted.sort(Comparator.comparing(r->r.startAt().toInstant()));
        for(int i=1;i<sorted.size();i++) if(sorted.get(i).startAt().toInstant().isBefore(sorted.get(i-1).endAt().toInstant())) throw new BadRequestException("Các khung giờ không được chồng lấn.");
    }
    @Transactional
    public InterviewProposalResponse propose(AuthPrincipal actor,UUID roundId,List<InterviewSlotRequest> requests) {
        validate(requests);var c=locked(actor,roundId,true);
        if(!slots.findByRoundIdOrderByStartAtAsc(roundId).isEmpty()) throw new ConflictException("Đã có đề xuất. Vui lòng dùng đổi lịch.");
        return createProposal(c,requests);
    }
    InterviewProposalResponse createProposal(Context c,List<InterviewSlotRequest> requests) {
        validate(requests);UUID proposal=UUID.randomUUID();Instant now=clock.instant();
        var list=new ArrayList<InterviewSlotResponse>();
        for(var r:requests) {var s=new InterviewSlot();s.setId(UUID.randomUUID());s.setRoundId(c.round().getId());s.setProposalId(proposal);s.setStartAt(r.startAt().toInstant());s.setEndAt(r.endAt().toInstant());s.setStatus("OPEN");s.setCreatedAt(now);slots.save(s);list.add(slotResponse(s));}
        var candidate=identities.get(c.application().getUserId());
        notifications.enqueue("proposal:"+proposal,"INVITATION",new InterviewNotificationPayload(null,c.round().getId(),c.round().getName(),c.application().getCompanyName(),candidate.fullName(),candidate.email(),c.round().getInterviewerEmail(),null,null,null),List.of(candidate.email()),now);
        return new InterviewProposalResponse(proposal,c.round().getId(),list);
    }
    @Transactional
    public InterviewProposalResponse reschedule(AuthPrincipal actor,UUID roundId,List<InterviewSlotRequest> requests) {
        validate(requests);var c=locked(actor,roundId,true);
        lifecycle.cancelRound(roundId,clock.instant());
        return createProposal(c,requests);
    }
    @Transactional
    public InterviewScheduleResponse pick(AuthPrincipal actor,UUID roundId,UUID slotId) {
        var c=locked(actor,roundId,false);
        resources.lock(c.application().getUserId(),c.round().getInterviewerEmail());
        var slot=slots.findById(slotId).filter(s->s.getRoundId().equals(roundId)).orElseThrow(InterviewSchedulingService::missing);
        var existing=schedules.findByRoundIdAndStatus(roundId,"SCHEDULED");
        if(existing.isPresent() && existing.get().getSlotId().equals(slotId)) return scheduleResponse(existing.get(),slot);
        if(!"OPEN".equals(slot.getStatus()) || !slot.getStartAt().isAfter(clock.instant()) || existing.isPresent()) throw new ConflictException("Khung giờ không còn khả dụng.");
        boolean conflict=Boolean.TRUE.equals(jdbc.queryForObject("""
            select exists(select 1 from recruitment.interview_schedules s join recruitment.interview_slots t on t.id=s.slot_id
            where s.status='SCHEDULED' and (s.candidate_id=? or lower(s.interviewer_email)=?) and t.start_at<? and t.end_at>?)
            """,Boolean.class,c.application().getUserId(),InterviewResourceLock.email(c.round().getInterviewerEmail()),Timestamp.from(slot.getEndAt()),Timestamp.from(slot.getStartAt())));
        if(conflict) throw new ConflictException("Ứng viên hoặc người phỏng vấn đã có lịch trong khung giờ này.");
        var candidate=identities.get(c.application().getUserId());
        String url=meet.create(slotId,c.round().getName(),slot.getStartAt(),slot.getEndAt());
        if(!slot.getStartAt().isAfter(clock.instant())) throw new ConflictException("Khung giờ đã bắt đầu.");
        var s=new InterviewSchedule();s.setId(UUID.randomUUID());s.setRoundId(roundId);s.setSlotId(slotId);s.setMeetUrl(url);s.setStatus("SCHEDULED");s.setCandidateId(c.application().getUserId());s.setCandidateEmail(InterviewResourceLock.email(candidate.email()));s.setInterviewerEmail(InterviewResourceLock.email(c.round().getInterviewerEmail()));s.setCreatedAt(clock.instant());s.setReminder1hSuppressed(!slot.getStartAt().isAfter(clock.instant().plusSeconds(3600)));schedules.saveAndFlush(s);
        for(var other:slots.findByRoundIdOrderByStartAtAsc(roundId)) {if("OPEN".equals(other.getStatus())) {other.setStatus(other.getId().equals(slotId)?"PICKED":"CANCELLED");slots.save(other);}}
        c.round().setScheduledAt(slot.getStartAt());rounds.save(c.round());
        notifications.cancelPending(roundId);
        notifications.enqueue("schedule:"+s.getId(),"CONFIRMATION",new InterviewNotificationPayload(s.getId(),roundId,c.round().getName(),c.application().getCompanyName(),candidate.fullName(),candidate.email(),s.getInterviewerEmail(),slot.getStartAt(),slot.getEndAt(),url),List.of(s.getCandidateEmail(),s.getInterviewerEmail()),clock.instant());
        return scheduleResponse(s,slot);
    }
    public static InterviewSlotResponse slotResponse(InterviewSlot s){return new InterviewSlotResponse(s.getId(),s.getRoundId(),s.getProposalId(),s.getStartAt(),s.getEndAt(),s.getStatus());}
    public static InterviewScheduleResponse scheduleResponse(InterviewSchedule s,InterviewSlot t){return new InterviewScheduleResponse(s.getId(),s.getRoundId(),s.getSlotId(),t.getStartAt(),t.getEndAt(),s.getMeetUrl(),s.getStatus());}
}
