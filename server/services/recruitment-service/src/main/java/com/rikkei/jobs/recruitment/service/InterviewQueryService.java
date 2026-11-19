package com.rikkei.jobs.recruitment.service;
import com.rikkei.jobs.common.config.exception.*;
import com.rikkei.jobs.common.config.security.principle.*;
import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.recruitment.dto.response.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class InterviewQueryService {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    static void page(int page,int size){if(page<1 || size<1 || size>100)throw new BadRequestException("page/size không hợp lệ.");}
    public PageResponse<InterviewInvitationResponse> listMine(AuthPrincipal actor,int page,int size){
        if(actor==null || actor.getRole()!=Role.CANDIDATE)throw new ForbiddenException("Chỉ ứng viên được xem lời mời.");page(page,size);
        String where=" from recruitment.interview_rounds r join recruitment.applications a on a.id=r.application_id where a.user_id=? and exists(select 1 from recruitment.interview_slots t where t.round_id=r.id)";
        long total=jdbc.queryForObject("select count(*)"+where,Long.class,actor.getUserId());
        var rows=jdbc.queryForList("select r.id,r.name,r.interviewer_email,a.job_title,a.company_name,a.status,r.result"+where+" order by r.created_at desc,r.id limit ? offset ?",actor.getUserId(),size,(long)(page-1)*size);
        if(rows.isEmpty())return new PageResponse<>(List.of(),total,page,size);
        var ids=rows.stream().map(r->(UUID)r.get("id")).toList();
        var allSlots=slotMap(ids);var allSchedules=scheduleMap(ids);
        var items=rows.stream().map(r->{UUID id=(UUID)r.get("id");boolean eligible="INTERVIEW".equals(r.get("status"))&&"PENDING".equals(r.get("result"));
            var open=allSlots.getOrDefault(id,List.of()).stream().filter(t->eligible&&"OPEN".equals(t.status())&&t.startAt().isAfter(clock.instant())).toList();
            return new InterviewInvitationResponse(id,(String)r.get("name"),(String)r.get("job_title"),(String)r.get("company_name"),(String)r.get("interviewer_email"),open,allSchedules.get(id));}).toList();
        return new PageResponse<>(items,total,page,size);
    }
    public Map<UUID,List<InterviewSlotResponse>> slotMap(List<UUID> ids){
        var result=new HashMap<UUID,List<InterviewSlotResponse>>();if(ids.isEmpty())return result;
        jdbc.query("select * from recruitment.interview_slots where round_id in ("+marks(ids.size())+") order by start_at,id",(org.springframework.jdbc.core.RowCallbackHandler)rs->{var r=new InterviewSlotResponse(rs.getObject("id",UUID.class),rs.getObject("round_id",UUID.class),rs.getObject("proposal_id",UUID.class),rs.getTimestamp("start_at").toInstant(),rs.getTimestamp("end_at").toInstant(),rs.getString("status"));result.computeIfAbsent(r.roundId(),k->new ArrayList<>()).add(r);},ids.toArray());return result;
    }
    public Map<UUID,InterviewScheduleResponse> scheduleMap(List<UUID> ids){
        var result=new HashMap<UUID,InterviewScheduleResponse>();if(ids.isEmpty())return result;
        jdbc.query("select s.*,t.start_at,t.end_at from recruitment.interview_schedules s join recruitment.interview_slots t on t.id=s.slot_id where s.round_id in ("+marks(ids.size())+") order by s.created_at desc,s.id",(org.springframework.jdbc.core.RowCallbackHandler)rs->{var s=schedule(rs);result.putIfAbsent(s.roundId(),s);},ids.toArray());return result;
    }
    public PageResponse<InterviewCalendarItemResponse> listCompany(AuthPrincipal actor,Instant from,Instant to,String email,String status,int page,int size){
        if(actor==null || actor.getRole()!=Role.HR || actor.getCompanyId()==null)throw new ForbiddenException("Chỉ HR được xem lịch công ty.");page(page,size);
        if(from==null || to==null || !from.isBefore(to))throw new BadRequestException("Khoảng thời gian không hợp lệ.");
        if(status!=null && !Set.of("SCHEDULED","DONE","CANCELLED").contains(status))throw new BadRequestException("Trạng thái lịch không hợp lệ.");
        String where=" from recruitment.interview_schedules s join recruitment.interview_slots t on t.id=s.slot_id join recruitment.interview_rounds r on r.id=s.round_id join recruitment.applications a on a.id=r.application_id where a.company_id=? and t.start_at<? and t.end_at>?";
        var args=new ArrayList<Object>(List.of(actor.getCompanyId(),Timestamp.from(to),Timestamp.from(from)));
        if(email!=null&&!email.isBlank()){where+=" and lower(s.interviewer_email)=?";args.add(InterviewResourceLock.email(email));}
        if(status!=null){where+=" and s.status=?";args.add(status);}else where+=" and s.status in ('SCHEDULED','DONE')";
        long total=jdbc.queryForObject("select count(*)"+where,Long.class,args.toArray());args.add(size);args.add((long)(page-1)*size);
        var items=jdbc.query("select s.*,t.start_at,t.end_at,a.job_title,r.name,coalesce((select n.payload::jsonb->>'candidateName' from recruitment.interview_notification_outbox n where n.schedule_id=s.id and n.type='CONFIRMATION' limit 1),s.candidate_id::text) candidate_name"+where+" order by t.start_at,s.id limit ? offset ?",(rs,i)->new InterviewCalendarItemResponse(schedule(rs),rs.getObject("candidate_id",UUID.class),rs.getString("candidate_name"),rs.getString("job_title"),rs.getString("name"),rs.getString("interviewer_email")),args.toArray());
        return new PageResponse<>(items,total,page,size);
    }
    static String marks(int count){return String.join(",",Collections.nCopies(count,"?"));}
    static InterviewScheduleResponse schedule(ResultSet rs)throws SQLException{return new InterviewScheduleResponse(rs.getObject("id",UUID.class),rs.getObject("round_id",UUID.class),rs.getObject("slot_id",UUID.class),rs.getTimestamp("start_at").toInstant(),rs.getTimestamp("end_at").toInstant(),"CANCELLED".equals(rs.getString("status"))?null:rs.getString("meet_url"),rs.getString("status"));}
}
