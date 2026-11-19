package com.rikkei.jobs.recruitment;
import com.fasterxml.jackson.databind.JsonNode;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.recruitment.client.*;
import com.rikkei.jobs.recruitment.service.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class InterviewLiveFlowTest extends ApplicationIntegrationSupport {
    static final TestSmtpServer smtp=new TestSmtpServer();
    static Process mock;
    static int mockPort;
    @LocalServerPort int port;
    @MockBean IdentityCandidateClient identities;
    @Autowired InterviewNotificationWorker worker;
    @Autowired InterviewReminderJob reminders;
    final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    @DynamicPropertySource static void boundaries(DynamicPropertyRegistry p)throws Exception {
        try(var s=new ServerSocket(0)){mockPort=s.getLocalPort();}
        var builder=new ProcessBuilder("node","server.js").directory(Path.of("../../..","mock").toFile()).redirectErrorStream(true).redirectOutput(Path.of("target/ut06-live-mock.log").toFile());
        builder.environment().put("PORT",Integer.toString(mockPort));builder.environment().put("SERVICE_KEY","ut06-live-key");mock=builder.start();
        var client=HttpClient.newHttpClient();boolean ready=false;
        for(int i=0;i<100;i++)try{client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+mockPort+"/mock/requests")).timeout(Duration.ofSeconds(1)).GET().build(),HttpResponse.BodyHandlers.discarding());ready=true;break;}catch(Exception ex){Thread.sleep(50);}
        if(!ready)throw new IllegalStateException("Mock did not start");
        p.add("app.recruitment.meet-base-url",()->"http://localhost:"+mockPort);p.add("app.recruitment.meet-service-key",()->"ut06-live-key");p.add("spring.mail.host",()->"localhost");p.add("spring.mail.port",smtp::port);
    }
    @AfterAll static void closeBoundaries()throws Exception{if(mock!=null)mock.destroy();smtp.close();}
    HttpResponse<String> call(String method,String path,String token,String body)throws Exception{
        var request=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/recruitment"+path)).header("Authorization",token).header("Content-Type","application/json").timeout(Duration.ofSeconds(15)).method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build();return http.send(request,HttpResponse.BodyHandlers.ofString());
    }
    @Test void realHttpMeetAndSmtpCoverRollbackRetryBookingRemindersAndCancellation()throws Exception{
        when(identities.get(any())).thenAnswer(a->new CandidateIdentity(a.getArgument(0),"Candidate","candidate@test.local"));
        UUID app=apply(),round=UUID.randomUUID();jdbc.update("update recruitment.applications set status='INTERVIEW' where id=?",app);
        jdbc.update("insert into recruitment.interview_rounds(id,application_id,round_no,name,interviewer_email,result,created_at) values (?,?,1,'Technical','hr@test.local','PENDING',?)",round,app,Timestamp.from(NOW));
        String hr=token(UUID.randomUUID(),Role.HR,company);
        String slots="[{\"startAt\":\""+NOW.plusSeconds(7200)+"\",\"endAt\":\""+NOW.plusSeconds(9000)+"\"},{\"startAt\":\""+NOW.plusSeconds(10800)+"\",\"endAt\":\""+NOW.plusSeconds(12600)+"\"}]";
        var proposal=call("POST","/rounds/"+round+"/slots",hr,slots);assertEquals(201,proposal.statusCode(),proposal.body());
        var proposed=json.readTree(proposal.body());String slot=proposed.get("slots").get(0).get("id").asText(),other=proposed.get("slots").get(1).get("id").asText();String body="{\"slotId\":\""+slot+"\"}";
        jdbc.execute("create or replace function recruitment.ut06_test_fail() returns trigger language plpgsql as $$ begin raise exception 'UT06 forced persistence failure'; end $$");
        jdbc.execute("create trigger ut06_test_fail before insert on recruitment.interview_schedules for each row execute function recruitment.ut06_test_fail()");
        try {assertEquals(500,call("POST","/me/interviews/"+round+"/pick",candidate(),body).statusCode());assertEquals(0,count("interview_schedules"));}
        finally{jdbc.execute("drop trigger ut06_test_fail on recruitment.interview_schedules");jdbc.execute("drop function recruitment.ut06_test_fail()");}
        var booked=call("POST","/me/interviews/"+round+"/pick",candidate(),body);assertEquals(200,booked.statusCode(),booked.body());String url=json.readTree(booked.body()).get("meetUrl").asText();assertTrue(url.startsWith("https://meet.rikkei.test/r/"));
        var replay=http.send(HttpRequest.newBuilder(URI.create("http://localhost:"+mockPort+"/meet/rooms")).header("X-Service-Key","ut06-live-key").header("Idempotency-Key",slot).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{\"title\":\"Technical\",\"startAt\":\""+NOW.plusSeconds(7200)+"\",\"endAt\":\""+NOW.plusSeconds(9000)+"\"}")).build(),HttpResponse.BodyHandlers.ofString());assertEquals(url,json.readTree(replay.body()).get("roomUrl").asText());
        assertEquals(409,call("POST","/me/interviews/"+round+"/pick",candidate(),"{\"slotId\":\""+other+"\"}").statusCode());
        worker.dispatchBatch();assertEquals(2,smtp.messages.size());
        for(byte[] raw:smtp.messages){var msg=new jakarta.mail.internet.MimeMessage(jakarta.mail.Session.getInstance(new Properties()),new ByteArrayInputStream(raw));assertTrue(msg.getSubject().contains("Xác nhận"));assertTrue(new String(raw,java.nio.charset.StandardCharsets.US_ASCII).contains("interview.ics"));}
        var calendar=call("GET","/companies/me/interviews?from=2030-01-01T00:00:00Z&to=2030-01-02T00:00:00Z",hr,null);assertEquals(1,json.readTree(calendar.body()).get("total").asInt());
        when(clock.instant()).thenReturn(NOW.plusSeconds(3600));reminders.enqueueDue();worker.dispatchBatch();assertEquals(4,smtp.messages.size());reminders.enqueueDue();worker.dispatchBatch();assertEquals(4,smtp.messages.size());
        String replacement="[{\"startAt\":\""+NOW.plusSeconds(18000)+"\",\"endAt\":\""+NOW.plusSeconds(19800)+"\"}]";
        assertEquals(200,call("POST","/rounds/"+round+"/reschedule",hr,replacement).statusCode());worker.dispatchBatch();assertEquals(7,smtp.messages.size());
        assertEquals("CANCELLED",jdbc.queryForObject("select status from recruitment.interview_schedules",String.class));
    }
}
