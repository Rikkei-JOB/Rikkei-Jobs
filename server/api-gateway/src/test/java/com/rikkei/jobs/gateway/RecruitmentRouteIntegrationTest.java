package com.rikkei.jobs.gateway;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties="logging.level.root=WARN")
class RecruitmentRouteIntegrationTest {
    private record Received(String method, String path, String query, String authorization, String body) {}
    private static final AtomicReference<Received> received = new AtomicReference<>();
    private static final HttpServer recruitment = stub();
    @LocalServerPort int port;
    @DynamicPropertySource static void address(DynamicPropertyRegistry registry) {
        registry.add("RECRUITMENT_SERVICE_URL", () -> "http://localhost:" + recruitment.getAddress().getPort());
    }
    @AfterAll static void stop() { recruitment.stop(0); }
    @Test void forwardsApplyBodyAndCandidateAuthorization() throws Exception {
        String body = "{\"cvId\":\"example\",\"coverLetter\":\"hello\"}";
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/recruitment/jobs/job-id/apply"))
            .timeout(Duration.ofSeconds(10)).header("Authorization", "Bearer candidate-token").header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(received.get()).isEqualTo(new Received("POST", "/recruitment/jobs/job-id/apply", null, "Bearer candidate-token", body));
    }
    @Test void forwardsListFilterWithoutLosingPagination() throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/recruitment/me/applications?status=REJECTED&page=2"))
            .timeout(Duration.ofSeconds(10)).header("Authorization", "Bearer candidate-token").GET().build();
        var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(received.get().path()).isEqualTo("/recruitment/me/applications");
        assertThat(received.get().query()).isEqualTo("status=REJECTED&page=2");
        assertThat(received.get().authorization()).isEqualTo("Bearer candidate-token");
    }
    private static HttpServer stub() {
        try {
            var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                received.set(new Received(exchange.getRequestMethod(), exchange.getRequestURI().getRawPath(), exchange.getRequestURI().getRawQuery(),
                    exchange.getRequestHeaders().getFirst("Authorization"), new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
                byte[] body = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(exchange.getRequestMethod().equals("POST") ? 201 : 200, body.length);
                try (var output = exchange.getResponseBody()) { output.write(body); }
            });
            server.start(); return server;
        } catch (IOException error) { throw new ExceptionInInitializerError(error); }
    }
}
