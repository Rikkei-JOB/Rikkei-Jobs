package com.rikkei.jobs.recruitment;

import com.rikkei.jobs.common.config.security.jwt.JwtUtils;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import static org.assertj.core.api.Assertions.*;

/** Real HTTP requests, JWT verification, RestClient dependencies, PostgreSQL and commit boundaries. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "app.jwt.secret=test_secret_test_secret_test_secret_test_secret",
    "app.recruitment.outbox-enabled=false","app.recruitment.interview-jobs-enabled=false",
    "app.recruitment.identity-service-key=ut03-live","app.recruitment.profile-service-key=ut03-live",
    "app.recruitment.company-service-key=ut03-live"})
class ContactLiveFlowTest {
    static final UUID A=UUID.randomUUID(),B=UUID.randomUUID(),HR_A=UUID.randomUUID(),HR_B=UUID.randomUUID(),
        USER=UUID.randomUUID(),STAFF=UUID.randomUUID(),JOB=UUID.randomUUID();
    static final ObjectMapper JSON=new ObjectMapper();
    static final HttpServer upstream=upstream();
    @LocalServerPort int port;
    @Autowired JwtUtils jwt;
    @Autowired JdbcTemplate jdbc;
    @DynamicPropertySource static void config(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url",()->System.getenv().getOrDefault("UT01_TEST_DB_URL","jdbc:postgresql://localhost:55439/ut03_test"));
        p.add("spring.datasource.username",()->System.getenv().getOrDefault("UT01_TEST_DB_USER","postgres"));
        p.add("spring.datasource.password",()->System.getenv().getOrDefault("UT01_TEST_DB_PASSWORD","ut03-test-only"));
        for(String service:List.of("identity","company","profile"))
            p.add("app.recruitment."+service+"-base-url",()->"http://localhost:"+upstream.getAddress().getPort());
    }
    @AfterAll static void stop(){upstream.stop(0);}
    HttpResponse<String> call(String method,String path,UUID actor,Role role,UUID company,String body) throws Exception {
        var request=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).timeout(Duration.ofSeconds(15))
            .header("Authorization","Bearer "+jwt.generateJwtToken(actor,role,company)).header("Content-Type","application/json")
            .method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build();
        return HttpClient.newHttpClient().send(request,HttpResponse.BodyHandlers.ofString());
    }
    @Test void realHttpRequestApprovalRevealAndCandidateHistory() throws Exception {
        String contact="/candidates/"+USER+"/contact";
        assertThat(call("GET",contact,HR_B,Role.HR,B,null).statusCode()).isEqualTo(403);
        var created=call("POST","/candidates/"+USER+"/contact-requests",HR_B,Role.HR,B,
            "{\"jobId\":\""+JOB+"\",\"reason\":\"Trao đổi công việc\"}");
        assertThat(created.statusCode()).isEqualTo(201);
        String id=JSON.readTree(created.body()).get("id").asText();
        assertThat(call("POST","/staff/contact-requests/"+id+"/approve",STAFF,Role.STAFF,null,null).statusCode()).isEqualTo(200);
        for(int i=0;i<2;i++){
            var revealed=call("GET",contact,HR_B,Role.HR,B,null);
            assertThat(revealed.statusCode()).isEqualTo(200);
            assertThat(JSON.readTree(revealed.body()).get("email").asText()).isEqualTo("live-candidate@example.test");
        }
        assertThat(call("GET",contact,HR_A,Role.HR,A,null).statusCode()).isEqualTo(403);
        var history=call("GET","/me/contact-access-logs",USER,Role.CANDIDATE,null,null);
        assertThat(history.statusCode()).isEqualTo(200);
        var rows=JSON.readTree(history.body());assertThat(rows.get("total").asInt()).isEqualTo(2);
        assertThat(rows.get("items").get(0).get("companyId").asText()).isEqualTo(B.toString());
        assertThat(rows.get("items").get(0).get("companyName").asText()).isEqualTo("Company B");
        assertThat(jdbc.queryForObject("select count(*) from recruitment.contact_access_logs where user_id=?",Integer.class,USER)).isEqualTo(2);
        jdbc.update("delete from recruitment.contact_access_logs where user_id=?",USER);
        jdbc.update("delete from recruitment.contact_requests where user_id=?",USER);
    }
    static HttpServer upstream() {
        try {
            var server=HttpServer.create(new InetSocketAddress("localhost",0),0);
            server.createContext("/internal/",e->{
                try {
                    if(!"ut03-live".equals(e.getRequestHeaders().getFirst("X-Internal-Key"))){e.sendResponseHeaders(401,-1);return;}
                    String path=e.getRequestURI().getPath();Object data;
                    if(path.equals("/internal/users/lookup")){
                        var input=JSON.readTree(e.getRequestBody());UUID hr=UUID.fromString(input.get("userIds").get(0).asText());
                        data=List.of(Map.of("userId",hr,"role","HR","companyId",hr.equals(HR_A)?A:B,"status","ACTIVE"));
                    }else if(path.equals("/internal/users/"+USER)){
                        data=Map.of("userId",USER,"fullName","Live candidate","email","live-candidate@example.test","role","CANDIDATE");
                    }else if(path.endsWith("/contact-metadata")){
                        data=Map.of("userId",USER,"fullName","Live candidate","visibility","PUBLIC");
                    }else if(path.endsWith("/phone")){
                        data=Map.of("userId",USER,"phone","0901234567");
                    }else if(path.endsWith("/application-context")){
                        data=Map.of("jobId",JOB,"companyId",B,"jobTitle","Java","companyName","Company B",
                            "status","APPROVED","deadline","2035-01-01T00:00:00Z","companyLocked",false,"hrEmails",List.of());
                    }else if(path.equals("/internal/contact-directory/lookup")){
                        data=Map.of("companies",Map.of(B.toString(),"Company B"),"hrs",Map.of(HR_B.toString(),"HR B"));
                    }else{e.sendResponseHeaders(404,-1);return;}
                    byte[] bytes=JSON.writeValueAsBytes(data);
                    e.getResponseHeaders().set("Content-Type","application/json");e.sendResponseHeaders(200,bytes.length);
                    try(var out=e.getResponseBody()){out.write(bytes);}
                }finally{e.close();}
            });
            server.start();return server;
        }catch(Exception e){throw new IllegalStateException(e);}
    }
}
