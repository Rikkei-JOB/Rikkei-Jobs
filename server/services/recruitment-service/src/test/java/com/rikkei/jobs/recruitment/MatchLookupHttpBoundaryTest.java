package com.rikkei.jobs.recruitment;
import com.rikkei.jobs.recruitment.client.HttpJobMatchClient;
import com.rikkei.jobs.recruitment.dto.response.MatchStatus;
import com.rikkei.jobs.recruitment.service.MatchLookupService;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import static org.junit.jupiter.api.Assertions.*;

class MatchLookupHttpBoundaryTest {
    @Test void realHttpPreservesBearerAndDecimalAndIsolatesUpstreamFailures()throws Exception {
        UUID job=UUID.randomUUID(),user=UUID.randomUUID();
        var status=new AtomicInteger(200);var body=new AtomicReference<>("{\"score\":78,\"belowMinimum\":false,\"met\":[{\"skill\":\"Java\",\"required\":true,\"minScore\":80,\"candidateScore\":82.058824}],\"missing\":[]}");
        var bearer=new AtomicReference<String>();var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/jobs/"+job+"/match/"+user,e->{bearer.set(e.getRequestHeaders().getFirst("Authorization"));byte[] bytes=body.get().getBytes(StandardCharsets.UTF_8);e.getResponseHeaders().set("Content-Type","application/json");e.sendResponseHeaders(status.get(),bytes.length);e.getResponseBody().write(bytes);e.close();});server.start();
        try {
            var service=new MatchLookupService(new HttpJobMatchClient(RestClient.builder(),"http://127.0.0.1:"+server.getAddress().getPort(),1000,1000));
            var result=service.lookup(job,user,"Bearer original-token");assertEquals(MatchStatus.AVAILABLE,result.matchStatus());assertEquals("82.058824",result.match().met().get(0).candidateScore().toPlainString());assertEquals("Bearer original-token",bearer.get());
            for(int code:new int[]{404,503,401,403}){status.set(code);assertEquals(MatchStatus.UNAVAILABLE,service.lookup(job,user,"Bearer original-token").matchStatus());}
            status.set(200);body.set("{}");assertEquals(MatchStatus.UNAVAILABLE,service.lookup(job,user,"Bearer original-token").matchStatus());
        }finally{server.stop(0);}
    }
    @Test void timeoutReturnsUnavailableBeforeSlowProviderCompletes()throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/jobs",e->{entered.countDown();try{release.await(5,TimeUnit.SECONDS);}catch(InterruptedException ex){Thread.currentThread().interrupt();}finally{e.close();}});server.start();
        var pool=Executors.newSingleThreadExecutor();
        try{
            var service=new MatchLookupService(new HttpJobMatchClient(RestClient.builder(),"http://127.0.0.1:"+server.getAddress().getPort(),1000,100));
            var lookup=pool.submit(()->service.lookup(UUID.randomUUID(),UUID.randomUUID(),"Bearer hr"));assertTrue(entered.await(2,TimeUnit.SECONDS));
            assertEquals(MatchStatus.UNAVAILABLE,lookup.get(2,TimeUnit.SECONDS).matchStatus());assertEquals(1,release.getCount());
        }finally{release.countDown();server.stop(0);pool.shutdownNow();}
    }
    @Test void timeoutAlsoCoversAStalledJsonBodyAfterHeaders()throws Exception {
        var flushed=new CountDownLatch(1);var release=new CountDownLatch(1);var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/jobs",e->{try{e.getResponseHeaders().set("Content-Type","application/json");e.sendResponseHeaders(200,0);e.getResponseBody().write('{');e.getResponseBody().flush();flushed.countDown();release.await(5,TimeUnit.SECONDS);}catch(InterruptedException ex){Thread.currentThread().interrupt();}finally{e.close();}});server.start();
        var pool=Executors.newSingleThreadExecutor();
        try {
            var service=new MatchLookupService(new HttpJobMatchClient(RestClient.builder(),"http://127.0.0.1:"+server.getAddress().getPort(),1000,250));
            var lookup=pool.submit(()->service.lookup(UUID.randomUUID(),UUID.randomUUID(),"Bearer hr"));assertTrue(flushed.await(2,TimeUnit.SECONDS));
            assertEquals(MatchStatus.UNAVAILABLE,lookup.get(2,TimeUnit.SECONDS).matchStatus());assertEquals(1,release.getCount());
        }finally{release.countDown();server.stop(0);pool.shutdownNow();}
    }
    @Test void missingOrNullBooleanFlagsAreContractFailures()throws Exception {
        var body=new AtomicReference<String>();var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/jobs",e->{byte[] b=body.get().getBytes(StandardCharsets.UTF_8);e.getResponseHeaders().set("Content-Type","application/json");e.sendResponseHeaders(200,b.length);e.getResponseBody().write(b);e.close();});server.start();
        try {
            var service=new MatchLookupService(new HttpJobMatchClient(RestClient.builder(),"http://127.0.0.1:"+server.getAddress().getPort(),1000,1000));
            for(String json:new String[]{"{\"score\":0,\"met\":[],\"missing\":[]}","{\"score\":0,\"belowMinimum\":null,\"met\":[],\"missing\":[]}","{\"score\":0,\"belowMinimum\":false,\"met\":[{\"skill\":\"Java\",\"minScore\":80,\"candidateScore\":82}],\"missing\":[]}","{\"score\":0,\"belowMinimum\":false,\"met\":[{\"skill\":\"Java\",\"required\":null,\"minScore\":80,\"candidateScore\":82}],\"missing\":[]}"}) {
                body.set(json);assertEquals(MatchStatus.UNAVAILABLE,service.lookup(UUID.randomUUID(),UUID.randomUUID(),"Bearer hr").matchStatus(),json);
            }
        }finally{server.stop(0);}
    }
}
