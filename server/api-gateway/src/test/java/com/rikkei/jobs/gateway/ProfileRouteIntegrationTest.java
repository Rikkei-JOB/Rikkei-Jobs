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
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"logging.level.root=WARN", "debug=false"})
class ProfileRouteIntegrationTest {
    private static final AtomicReference<ForwardedRequest> RECEIVED = new AtomicReference<>();
    private static final HttpServer PROFILE = profileStub();
    private static final String RESPONSE = "[{\"skill\":{\"code\":\"JAVA\"}}]";

    @LocalServerPort int port;

    @DynamicPropertySource
    static void profileAddress(DynamicPropertyRegistry registry) {
        registry.add("PROFILE_SERVICE_URL", () -> "http://localhost:" + PROFILE.getAddress().getPort());
    }

    @AfterAll
    static void stopProfile() {
        PROFILE.stop(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"profile", "profiles"})
    void profileRoutesForwardPathQueryAndAuthorization(String area) throws Exception {
        RECEIVED.set(null);
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port
                        + "/api/v1/" + area + "/me/skills?group=Backend&level=GOOD"))
                .header("Authorization", "Bearer profile-test-token")
                .timeout(Duration.ofSeconds(10)).GET().build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo(RESPONSE);
        assertThat(RECEIVED.get()).isEqualTo(new ForwardedRequest("/" + area + "/me/skills",
                "group=Backend&level=GOOD", "Bearer profile-test-token"));
    }

    private static HttpServer profileStub() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                RECEIVED.set(new ForwardedRequest(exchange.getRequestURI().getRawPath(),
                        exchange.getRequestURI().getRawQuery(), exchange.getRequestHeaders().getFirst("Authorization")));
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

    private record ForwardedRequest(String path, String query, String authorization) { }
}
