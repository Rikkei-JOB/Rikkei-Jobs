package com.rikkei.jobs.profile.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.rikkei.jobs.profile.client.ExternalSourceClient;
import com.rikkei.jobs.profile.client.ExternalSourceException;
import com.rikkei.jobs.profile.config.SyncProperties;
import com.rikkei.jobs.profile.entity.MappingSource;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/** Client gọi mock: X-Service-Key, thử lại 2 lần khi lỗi mạng hoặc 5xx, không thử lại 4xx, timeout. */
class ExternalSourceClientTest {
    private final AtomicInteger hits = new AtomicInteger();
    private final AtomicReference<String> serviceKey = new AtomicReference<>();
    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsServiceKeyAndReturnsItems() {
        ExternalSourceClient client = client(serve(200, "[{\"subjectCode\":\"JAVA01\"},{\"subjectCode\":\"SQL01\"}]"));

        List<JsonNode> items = client.lmsResults("lms-sv-001");

        assertThat(items).hasSize(2);
        assertThat(serviceKey.get()).isEqualTo("dev-key");
        assertThat(hits.get()).isEqualTo(1);
    }

    @Test
    void mockTurnedOffRetriesTwiceThenFails() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }

        assertThatThrownBy(() -> client(closedPort).examResults("lms-sv-001"))
                .isInstanceOf(ExternalSourceException.class)
                .hasMessage("Không kết nối được hệ thống Khảo thí (đã thử 3 lần)")
                .extracting(e -> ((ExternalSourceException) e).getSource()).isEqualTo(MappingSource.EXAM);
    }

    @Test
    void serverErrorIsRetriedTwice() {
        ExternalSourceClient client = client(serve(503, "{\"code\":\"DOWN\"}"));

        assertThatThrownBy(() -> client.internships("lms-sv-001"))
                .hasMessage("lms-pm trả lỗi HTTP 503 (đã thử 3 lần)");
        assertThat(hits.get()).isEqualTo(3);
    }

    @Test
    void clientErrorIsNotRetried() {
        ExternalSourceClient client = client(serve(401, "{\"code\":\"INVALID_SERVICE_KEY\"}"));

        assertThatThrownBy(() -> client.lmsResults("lms-sv-001")).hasMessage("LMS trả lỗi HTTP 401");
        assertThat(hits.get()).isEqualTo(1);
    }

    @Test
    void nonArrayBodyIsRejected() {
        ExternalSourceClient client = client(serve(200, "{\"subjectCode\":\"JAVA01\"}"));

        assertThatThrownBy(() -> client.lmsResults("lms-sv-001")).hasMessage("LMS trả dữ liệu không phải danh sách");
    }

    @Test
    void slowResponseTimesOut() {
        server = start(exchange -> {
            hits.incrementAndGet();
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.close();
        });
        SyncProperties properties = properties(server.getAddress().getPort());
        properties.setTimeout(Duration.ofMillis(200));
        properties.setRetries(0);

        assertThatThrownBy(() -> new ExternalSourceClient(RestClient.builder(), properties).lmsResults("lms-sv-001"))
                .hasMessage("Không kết nối được LMS");
    }

    private int serve(int status, String body) {
        server = start(exchange -> {
            hits.incrementAndGet();
            serviceKey.set(exchange.getRequestHeaders().getFirst("X-Service-Key"));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (var output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        });
        return server.getAddress().getPort();
    }

    private static HttpServer start(com.sun.net.httpserver.HttpHandler handler) {
        try {
            HttpServer created = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            created.createContext("/", handler);
            created.start();
            return created;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ExternalSourceClient client(int port) {
        return new ExternalSourceClient(RestClient.builder(), properties(port));
    }

    private static SyncProperties properties(int port) {
        SyncProperties properties = new SyncProperties();
        properties.setBaseUrl("http://localhost:" + port);
        properties.setRetryBackoff(Duration.ZERO);
        return properties;
    }
}
