package com.rikkei.jobs.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rikkei.jobs.company.client.HttpProfileSkillClient;
import com.rikkei.jobs.company.config.AppProperties;
import com.rikkei.jobs.company.config.exception.CompanyErrorCode;
import com.rikkei.jobs.company.config.exception.CompanyException;
import com.sun.net.httpserver.HttpServer;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class HttpProfileSkillClientTest {

    private static final String MESSAGE = "Không lấy được điểm kỹ năng của ứng viên, vui lòng thử lại.";

    private HttpServer server;
    private final AtomicInteger calls = new AtomicInteger();
    private final AtomicReference<String> auth = new AtomicReference<>();
    private final AtomicReference<String> key = new AtomicReference<>();
    private final AtomicReference<String> path = new AtomicReference<>();

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private HttpProfileSkillClient start(int status, String body, String serviceKey) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            calls.incrementAndGet();
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            key.set(exchange.getRequestHeaders().getFirst("X-Internal-Key"));
            path.set(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
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
        props.getProfile().setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        props.getInternal().setServiceKey(serviceKey);
        return new HttpProfileSkillClient(props);
    }

    private static void assertUnavailable(Throwable e) {
        assertThat(e).isInstanceOfSatisfying(CompanyException.class, ex -> {
            assertThat(ex.getCode()).isEqualTo(CompanyErrorCode.EXTERNAL_UNAVAILABLE);
            assertThat(ex.getMessage()).isEqualTo(MESSAGE);
        });
    }

    @Test
    void sendsKeyAndAuthorizationAndParsesScores() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID java = UUID.randomUUID();
        String body = "[{\"skillId\":\"" + java + "\",\"verifiedScore\":82.058824,"
                + "\"updatedAt\":\"2026-11-01T10:00:00Z\"}]";
        HttpProfileSkillClient client = start(200, body, "k-1");

        Map<UUID, BigDecimal> scores = client.verifiedScores(userId, "Bearer abc");

        assertThat(path.get()).isEqualTo("GET /internal/profiles/" + userId + "/verified-skills");
        assertThat(auth.get()).isEqualTo("Bearer abc");
        assertThat(key.get()).isEqualTo("k-1");
        assertThat(scores).containsOnlyKeys(java);
        assertThat(scores.get(java)).isEqualByComparingTo("82.058824");
    }

    @Test
    void emptyArrayGivesEmptyMap() throws Exception {
        HttpProfileSkillClient client = start(200, "[]", "k-1");

        assertThat(client.verifiedScores(UUID.randomUUID(), "Bearer abc")).isEmpty();
    }

    @Test
    void non2xxIsUnavailable() throws Exception {
        HttpProfileSkillClient unauthorized = start(401, "{}", "k-1");
        assertThatThrownBy(() -> unauthorized.verifiedScores(UUID.randomUUID(), "Bearer x"))
                .satisfies(HttpProfileSkillClientTest::assertUnavailable);
        server.stop(0);

        HttpProfileSkillClient failing = start(500, "{}", "k-1");
        assertThatThrownBy(() -> failing.verifiedScores(UUID.randomUUID(), "Bearer x"))
                .satisfies(HttpProfileSkillClientTest::assertUnavailable);
    }

    @Test
    void blankKeyIsUnavailableWithoutCalling() throws Exception {
        HttpProfileSkillClient client = start(200, "[]", " ");

        assertThatThrownBy(() -> client.verifiedScores(UUID.randomUUID(), "Bearer x"))
                .satisfies(HttpProfileSkillClientTest::assertUnavailable);
        assertThat(calls.get()).isZero();
    }

    @Test
    void connectionRefusedIsUnavailable() throws Exception {
        HttpProfileSkillClient client = start(200, "[]", "k-1");
        server.stop(0);

        assertThatThrownBy(() -> client.verifiedScores(UUID.randomUUID(), "Bearer x"))
                .satisfies(HttpProfileSkillClientTest::assertUnavailable);
    }
}
