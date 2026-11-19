package com.rikkei.jobs.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rikkei.jobs.company.client.HttpApplicationClient;
import com.rikkei.jobs.company.config.AppProperties;
import com.rikkei.jobs.company.config.exception.CompanyErrorCode;
import com.rikkei.jobs.company.config.exception.CompanyException;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class HttpApplicationClientTest {

    private static final String MESSAGE = "Không kiểm tra được đơn ứng tuyển, vui lòng thử lại.";

    private HttpServer server;
    private final AtomicInteger calls = new AtomicInteger();
    private final AtomicReference<String> auth = new AtomicReference<>();
    private final AtomicReference<String> key = new AtomicReference<>();
    private final AtomicReference<String> path = new AtomicReference<>();
    private final AtomicReference<String> query = new AtomicReference<>();

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private HttpApplicationClient start(int status, String body, String serviceKey) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            calls.incrementAndGet();
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            key.set(exchange.getRequestHeaders().getFirst("X-Internal-Key"));
            path.set(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            query.set(exchange.getRequestURI().getQuery());
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            if (bytes.length == 0) {
                exchange.sendResponseHeaders(status, -1);
            } else {
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
        server.start();
        AppProperties props = new AppProperties();
        props.getRecruitment().setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        props.getInternal().setServiceKey(serviceKey);
        return new HttpApplicationClient(props);
    }

    private static void assertUnavailable(Throwable e) {
        assertThat(e).isInstanceOfSatisfying(CompanyException.class, ex -> {
            assertThat(ex.getCode()).isEqualTo(CompanyErrorCode.EXTERNAL_UNAVAILABLE);
            assertThat(ex.getMessage()).isEqualTo(MESSAGE);
        });
    }

    @Test
    void sendsKeyAndParams() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        HttpApplicationClient client = start(200, "{\"applied\":true}", "k-1");

        assertThat(client.hasApplied(jobId, userId, "Bearer abc")).isTrue();

        assertThat(path.get()).isEqualTo("GET /internal/applications/exists");
        assertThat(query.get()).contains("jobId=" + jobId).contains("userId=" + userId);
        assertThat(auth.get()).isEqualTo("Bearer abc");
        assertThat(key.get()).isEqualTo("k-1");
        server.stop(0);

        HttpApplicationClient none = start(200, "{\"applied\":false}", "k-1");
        assertThat(none.hasApplied(jobId, userId, "Bearer abc")).isFalse();
    }

    @Test
    void non2xxIsUnavailable() throws Exception {
        HttpApplicationClient client = start(500, "{}", "k-1");

        assertThatThrownBy(() -> client.hasApplied(UUID.randomUUID(), UUID.randomUUID(), "Bearer x"))
                .satisfies(HttpApplicationClientTest::assertUnavailable);
    }

    @Test
    void missingAppliedFieldIsUnavailable() throws Exception {
        HttpApplicationClient client = start(200, "{}", "k-1");

        assertThatThrownBy(() -> client.hasApplied(UUID.randomUUID(), UUID.randomUUID(), "Bearer x"))
                .satisfies(HttpApplicationClientTest::assertUnavailable);
    }

    @Test
    void blankKeyIsUnavailableWithoutCalling() throws Exception {
        HttpApplicationClient client = start(200, "{\"applied\":true}", "");

        assertThatThrownBy(() -> client.hasApplied(UUID.randomUUID(), UUID.randomUUID(), "Bearer x"))
                .satisfies(HttpApplicationClientTest::assertUnavailable);
        assertThat(calls.get()).isZero();
    }
}
