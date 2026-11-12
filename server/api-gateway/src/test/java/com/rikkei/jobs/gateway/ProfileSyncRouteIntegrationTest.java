package com.rikkei.jobs.gateway;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/** NT-05: màn "Đồng bộ dữ liệu" gọi /api/v1/staff/sync..., /staff/external-records... và phải tới profile-service. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"logging.level.root=WARN", "debug=false"})
class ProfileSyncRouteIntegrationTest {
    private static final AtomicReference<String> RECEIVED_PATH = new AtomicReference<>();
    private static final AtomicInteger HITS = new AtomicInteger();
    private static final HttpServer PROFILE = profileStub();

    @LocalServerPort int port;

    @DynamicPropertySource
    static void profileAddress(DynamicPropertyRegistry registry) {
        registry.add("PROFILE_SERVICE_URL", () -> "http://localhost:" + PROFILE.getAddress().getPort());
        // Công ty trỏ vào cổng không ai nghe: request /staff/companies không được lọt sang profile-service.
        registry.add("COMPANY_SERVICE_URL", () -> "http://localhost:1");
    }

    @AfterAll
    static void stopProfile() {
        PROFILE.stop(0);
    }

    @BeforeEach
    void reset() {
        RECEIVED_PATH.set(null);
        HITS.set(0);
    }

    @ParameterizedTest
    @CsvSource({
            "/api/v1/staff/sync-runs?status=FAILED, /staff/sync-runs",
            "/api/v1/staff/sync/candidates?q=an, /staff/sync/candidates",
            "/api/v1/staff/external-records?userId=1, /staff/external-records"
    })
    void syncRoutesReachProfileService(String gatewayPath, String servicePath) throws Exception {
        HttpResponse<String> response = get(gatewayPath);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(RECEIVED_PATH.get()).isEqualTo(servicePath);
    }

    @Test
    void staffCompaniesStillGoToCompanyService() throws Exception {
        get("/api/v1/staff/companies");

        assertThat(HITS.get()).isZero();
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer staff-test-token")
                .timeout(Duration.ofSeconds(10)).GET().build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpServer profileStub() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                HITS.incrementAndGet();
                RECEIVED_PATH.set(exchange.getRequestURI().getRawPath());
                byte[] body = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (var output = exchange.getResponseBody()) {
                    output.write(body);
                }
            });
            server.start();
            return server;
        } catch (IOException error) {
            throw new ExceptionInInitializerError(error);
        }
    }
}
