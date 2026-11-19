package com.rikkei.jobs.recruitment;

import java.sql.Timestamp;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import static org.junit.jupiter.api.Assertions.*;

class InterviewSchedulingPersistenceTest extends ApplicationIntegrationSupport {
    UUID round() throws Exception {
        UUID r = UUID.randomUUID();
        jdbc.update("insert into recruitment.interview_rounds(id,application_id,round_no,name,interviewer_email,result,created_at) values (?,?,1,'Technical','hr@test.local','PENDING',?)",
            r, apply(), Timestamp.from(NOW));
        return r;
    }
    UUID slot(UUID r, int seconds) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into recruitment.interview_slots(id,round_id,proposal_id,start_at,end_at,status,created_at) values (?,?,?,?,?,'OPEN',?)",
            id,r,UUID.randomUUID(),Timestamp.from(NOW.plusSeconds(3600)),Timestamp.from(NOW.plusSeconds(3600+seconds)),Timestamp.from(NOW));
        return id;
    }
    void schedule(UUID r, UUID slot) {
        jdbc.update("insert into recruitment.interview_schedules(id,round_id,slot_id,meet_url,status,candidate_id,candidate_email,interviewer_email,created_at) values (?,?,?,'https://meet.test/room','SCHEDULED',?,'candidate@test.local','hr@test.local',?)",
            UUID.randomUUID(),r,slot,user,Timestamp.from(NOW));
    }
    @Test void rejectsShortSlotAndMismatchedRound() throws Exception {
        UUID r=round();
        assertThrows(DataIntegrityViolationException.class,()->slot(r,1799));
        UUID s=slot(r,1800);
        assertThrows(DataIntegrityViolationException.class,()->schedule(UUID.randomUUID(),s));
        schedule(r,s);
        assertEquals(1,count("interview_schedules"));
    }
    @Test void allowsOneActiveSchedulePerRound() throws Exception {
        UUID r=round(), first=slot(r,1800),second=slot(r,3600);
        schedule(r,first);
        assertThrows(DataIntegrityViolationException.class,()->schedule(r,second));
        jdbc.update("update recruitment.interview_schedules set status='CANCELLED' where round_id=?",r);
        schedule(r,second);
        assertEquals(2,count("interview_schedules"));
    }
}
