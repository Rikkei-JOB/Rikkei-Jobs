package com.rikkei.jobs.recruitment;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.recruitment.service.*;
import com.rikkei.jobs.recruitment.entity.InterviewNotificationOutbox;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class InterviewCalendarWriterTest {
    @Test void requestAndCancellationShareUidWithIncreasingSequenceAndUtcTimes() throws Exception {
        var mapper=new ObjectMapper().findAndRegisterModules();var writer=new InterviewCalendarWriter(mapper);
        UUID schedule=UUID.randomUUID();var payload=new InterviewNotificationPayload(schedule,UUID.randomUUID(),"Kỹ thuật, Java;\nLập trình","Company","Ứng viên","candidate@test.local","hr@test.local",Instant.parse("2030-01-01T09:00:00Z"),Instant.parse("2030-01-01T09:30:00Z"),"https://meet.test/room");
        var n=new InterviewNotificationOutbox();n.setType("CONFIRMATION");n.setRecipient("candidate@test.local");n.setPayload(mapper.writeValueAsString(payload));n.setCreatedAt(Instant.EPOCH);
        String request=new String(writer.write(n),java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(request.contains("METHOD:REQUEST\r\n"));assertTrue(request.contains("UID:"+schedule+"@rikkei-jobs"));assertTrue(request.contains("DTSTART:20300101T090000Z"));assertTrue(request.contains("SEQUENCE:0"));assertTrue(request.contains("Kỹ thuật\\, Java\\;\\nLập trình"));
        n.setType("CANCELLATION");String cancel=new String(writer.write(n),java.nio.charset.StandardCharsets.UTF_8);assertTrue(cancel.contains("METHOD:CANCEL"));assertTrue(cancel.contains("SEQUENCE:1"));assertTrue(cancel.contains("UID:"+schedule+"@rikkei-jobs"));
        for(String line:request.split("\r\n"))assertTrue(line.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=75);
    }
}
