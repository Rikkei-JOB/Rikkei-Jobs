package com.rikkei.jobs.recruitment;
import com.rikkei.jobs.recruitment.client.*;
import com.rikkei.jobs.common.config.security.principle.Role;
import java.sql.Timestamp;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

abstract class InterviewIntegrationSupport extends ApplicationIntegrationSupport {
    @MockBean MeetRoomClient meet;
    @MockBean IdentityCandidateClient identities;
    @MockBean JobMatchClient matches;
    UUID roundId,applicationId;
    @BeforeEach void setupInterview() throws Exception {
        when(identities.get(any())).thenAnswer(a->new CandidateIdentity(a.getArgument(0),"Candidate","candidate@test.local"));
        when(meet.create(any(),any(),any(),any())).thenReturn("https://meet.test/room");
        applicationId=apply();
        jdbc.update("update recruitment.applications set status='INTERVIEW' where id=?",applicationId);
        roundId=UUID.randomUUID();
        jdbc.update("insert into recruitment.interview_rounds(id,application_id,round_no,name,interviewer_email,result,created_at) values (?,?,1,'Technical','hr@test.local','PENDING',?)",roundId,applicationId,Timestamp.from(NOW));
    }
    String hr(){return token(UUID.randomUUID(),Role.HR,company);}
    String slots(long... offsets) {
        var result=new ArrayList<Map<String,String>>();
        for(long offset:offsets) result.add(Map.of("startAt",NOW.plusSeconds(offset).toString(),"endAt",NOW.plusSeconds(offset+1800).toString()));
        try{return json.writeValueAsString(result);}catch(Exception ex){throw new RuntimeException(ex);}
    }
    UUID propose(long... offsets) throws Exception {
        var text=mvc.perform(post("/recruitment/rounds/{r}/slots",roundId).header("Authorization",hr()).contentType("application/json").content(slots(offsets)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(text).get("slots").get(0).get("id").asText());
    }
    org.springframework.test.web.servlet.ResultActions pick(UUID slot) throws Exception {
        return mvc.perform(post("/recruitment/me/interviews/{r}/pick",roundId).header("Authorization",candidate()).contentType("application/json").content("{\"slotId\":\""+slot+"\"}"));
    }
}
