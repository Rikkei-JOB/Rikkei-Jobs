package com.rikkei.jobs.recruitment.client;
import com.rikkei.jobs.recruitment.config.RecruitmentDependencyException;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpMeetRoomClient implements MeetRoomClient {
    private final RestClient client;
    private final String key;
    private record Room(String roomUrl) {}
    public HttpMeetRoomClient(RestClient.Builder builder,
        @Value("${app.recruitment.meet-base-url:http://localhost:9000}") String base,
        @Value("${app.recruitment.meet-service-key:}") String key,
        @Value("${app.recruitment.meet-connect-timeout-ms:2000}") int connect,
        @Value("${app.recruitment.meet-read-timeout-ms:5000}") int read) {
        var factory=new SimpleClientHttpRequestFactory();factory.setConnectTimeout(connect);factory.setReadTimeout(read);
        this.client=builder.clone().baseUrl(base).requestFactory(factory).build();this.key=key;
    }
    public String create(UUID slotId,String title,Instant startAt,Instant endAt) {
        try {
            if(key.isBlank()) throw new IllegalStateException("Missing Meet key");
            var room=client.post().uri("/meet/rooms").header("X-Service-Key",key).header("Idempotency-Key",slotId.toString())
                .body(Map.of("title",title,"startAt",startAt.toString(),"endAt",endAt.toString())).retrieve().body(Room.class);
            if(room==null || room.roomUrl()==null) throw new IllegalStateException("Missing room URL");
            URI uri=URI.create(room.roomUrl());
            if(!Set.of("https","http").contains(uri.getScheme()) || uri.getHost()==null || uri.getUserInfo()!=null) throw new IllegalStateException("Invalid URL");
            return uri.toString();
        } catch(RuntimeException ex) {
            throw new RecruitmentDependencyException(503,"MEET_UNAVAILABLE","Không tạo được phòng họp. Vui lòng thử lại.");
        }
    }
}
