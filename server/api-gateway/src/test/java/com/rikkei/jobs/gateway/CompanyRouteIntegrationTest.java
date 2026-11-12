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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"logging.level.root=WARN", "debug=false"})
class CompanyRouteIntegrationTest {
    private static final AtomicReference<ForwardedRequest> RECEIVED = new AtomicReference<>();
    private static final AtomicInteger HITS = new AtomicInteger();
    private static final HttpServer COMPANY = companyStub();
    private static final String RESPONSE = "{\"ok\":true}";

    @LocalServerPort int port;

    @DynamicPropertySource
    static void companyAddress(DynamicPropertyRegistry registry) {
        registry.add("COMPANY_SERVICE_URL", () -> "http://localhost:" + COMPANY.getAddress().getPort());
    }

    @AfterAll
    static void stopCompany() {
        COMPANY.stop(0);
    }

    @BeforeEach
    void reset() {
        RECEIVED.set(null);
        HITS.set(0);
    }

    @ParameterizedTest
    @CsvSource({
            "/api/v1/companies/me, /companies/me",
            "/api/v1/staff/companies, /staff/companies",
            "/api/v1/company/ping, /company/ping"
    })
    void companyRoutesForwardPathAndAuthorization(String gatewayPath, String servicePath) throws Exception {
        HttpResponse<String> response = get(gatewayPath);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo(RESPONSE);
        assertThat(RECEIVED.get()).isEqualTo(new ForwardedRequest(servicePath, "Bearer company-test-token"));
    }

    @Test
    void internalApiIsNotExposedThroughGateway() throws Exception {
        HttpResponse<String> response = get("/api/v1/internal/users");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(HITS.get()).isZero();
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer company-test-token")
                .timeout(Duration.ofSeconds(10)).GET().build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpServer companyStub() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                HITS.incrementAndGet();
                RECEIVED.set(new ForwardedRequest(exchange.getRequestURI().getRawPath(),
                        exchange.getRequestHeaders().getFirst("Authorization")));
                byte[] body = RESPONSE.getBytes(StandardCharsets.UTF_8);
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

    private record ForwardedRequest(String path, String authorization) { }
}
