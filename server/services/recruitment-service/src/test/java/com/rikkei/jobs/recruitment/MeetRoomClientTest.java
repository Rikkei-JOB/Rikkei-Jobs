package com.rikkei.jobs.recruitment;
import com.rikkei.jobs.recruitment.client.HttpMeetRoomClient;
import com.rikkei.jobs.recruitment.config.RecruitmentDependencyException;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import static org.junit.jupiter.api.Assertions.*;
class MeetRoomClientTest {
    @Test void mapsRoomUrlAndSendsKeys() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        UUID slot=UUID.randomUUID();
        server.createContext("/meet/rooms",e->{
            boolean valid=slot.toString().equals(e.getRequestHeaders().getFirst("Idempotency-Key")) && "key".equals(e.getRequestHeaders().getFirst("X-Service-Key"));
            byte[] body="{\"roomUrl\":\"https://meet.test/room\"}".getBytes();
            e.getResponseHeaders().add("Content-Type","application/json");e.sendResponseHeaders(valid?200:401,body.length);e.getResponseBody().write(body);e.close();
        });server.start();
        try {
            var client=new HttpMeetRoomClient(RestClient.builder(),"http://127.0.0.1:"+server.getAddress().getPort(),"key",2000,5000);
            assertEquals("https://meet.test/room",client.create(slot,"Round",Instant.EPOCH,Instant.EPOCH.plusSeconds(1800)));
        } finally {server.stop(0);}
    }
    @Test void rejectsMissingUnsafeUrlAndUpstreamFailure() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/meet/rooms",e->{byte[] b="{\"roomUrl\":\"javascript:bad\"}".getBytes(); e.getResponseHeaders().add("Content-Type","application/json");e.sendResponseHeaders(200,b.length);e.getResponseBody().write(b);e.close();});server.start();
        var client=new HttpMeetRoomClient(RestClient.builder(),"http://127.0.0.1:"+server.getAddress().getPort(),"key",100,100);
        try {assertThrows(RecruitmentDependencyException.class,()->client.create(UUID.randomUUID(),"Round",Instant.EPOCH,Instant.EPOCH.plusSeconds(1800)));}finally{server.stop(0);}
        assertThrows(RecruitmentDependencyException.class,()->client.create(UUID.randomUUID(),"Round",Instant.EPOCH,Instant.EPOCH.plusSeconds(1800)));
    }
}
