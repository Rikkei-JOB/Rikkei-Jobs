package com.rikkei.jobs.recruitment;
import com.rikkei.jobs.common.config.security.principle.Role;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
class InterviewSchedulingConcurrencyTest extends InterviewIntegrationSupport {
    UUID secondRound(UUID candidate,String email) {
        UUID app=UUID.randomUUID(),r=UUID.randomUUID();
        jdbc.update("""
          insert into recruitment.applications(id,job_id,user_id,company_id,job_title,company_name,cv_id,cv_snapshot_url,status,created_at,updated_at)
          select ?,?,?,company_id,job_title,company_name,cv_id,cv_snapshot_url,'INTERVIEW',created_at,updated_at from recruitment.applications where id=?
          """,app,UUID.randomUUID(),candidate,applicationId);
        jdbc.update("insert into recruitment.interview_rounds(id,application_id,round_no,name,interviewer_email,result,created_at) values (?,?,1,'Other',?,'PENDING',?)",r,app,email,Timestamp.from(NOW));return r;
    }
    UUID open(UUID r,long offset) {
        UUID slot=UUID.randomUUID();
        jdbc.update("insert into recruitment.interview_slots(id,round_id,proposal_id,start_at,end_at,status,created_at) values (?,?,?,?,?,'OPEN',?)",slot,r,UUID.randomUUID(),Timestamp.from(NOW.plusSeconds(offset)),Timestamp.from(NOW.plusSeconds(offset+1800)),Timestamp.from(NOW));return slot;
    }
    int select(UUID r,UUID slot,UUID candidate) throws Exception {
        return mvc.perform(post("/recruitment/me/interviews/{r}/pick",r).header("Authorization",token(candidate,Role.CANDIDATE,null)).contentType("application/json").content("{\"slotId\":\""+slot+"\"}")).andReturn().getResponse().getStatus();
    }
    List<Integer> race(Callable<Integer> a,Callable<Integer> b) throws Exception {
        var gate=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try {var one=pool.submit(()->{gate.await();return a.call();});var two=pool.submit(()->{gate.await();return b.call();});gate.countDown();return List.of(one.get(20,TimeUnit.SECONDS),two.get(20,TimeUnit.SECONDS));}finally{pool.shutdownNow();}
    }
    @Test void sameInterviewerAcrossApplicationsCannotBeDoubleBooked() throws Exception {
        UUID other=UUID.randomUUID(),r=secondRound(other,"HR@TEST.LOCAL"),a=open(roundId,7200),b=open(r,7200);
        var result=race(()->select(roundId,a,user),()->select(r,b,other));
        assertEquals(Set.of(200,409),new HashSet<>(result));assertEquals(1,count("interview_schedules"));
    }
    @Test void sameCandidateAcrossRoundsCannotBeDoubleBooked() throws Exception {
        UUID r=secondRound(user,"other@test.local"),a=open(roundId,7200),b=open(r,7200);
        assertEquals(Set.of(200,409),new HashSet<>(race(()->select(roundId,a,user),()->select(r,b,user))));
    }
    @Test void sameRoundCreatesOnlyOneScheduleAndAdjacentBookingsAreAllowed() throws Exception {
        UUID a=open(roundId,7200),b=open(roundId,10800);
        assertEquals(Set.of(200,409),new HashSet<>(race(()->select(roundId,a,user),()->select(roundId,b,user))));
        UUID picked=jdbc.queryForObject("select slot_id from recruitment.interview_schedules",UUID.class);
        long next=picked.equals(a)?9000:12600;
        UUID other=UUID.randomUUID(),r=secondRound(other,"hr@test.local"),c=open(r,next);
        assertEquals(200,select(r,c,other));
    }
}
