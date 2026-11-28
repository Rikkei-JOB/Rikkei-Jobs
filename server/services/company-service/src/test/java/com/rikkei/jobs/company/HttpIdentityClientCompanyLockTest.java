package com.rikkei.jobs.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.company.client.HttpIdentityClient;
import com.rikkei.jobs.company.client.IdentityUserStatus;
import com.rikkei.jobs.company.config.AppProperties;
import com.rikkei.jobs.company.config.exception.CompanyErrorCode;
import com.rikkei.jobs.company.config.exception.CompanyException;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** DN-07 §4.5: company-service gọi POST /internal/users/company-lock của identity. */
class HttpIdentityClientCompanyLockTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String MESSAGE = "Không cập nhật được tài khoản HR, vui lòng thử lại.";

    private HttpServer server;
    private final AtomicReference<String> method = new AtomicReference<>();
    private final AtomicReference<String> path = new AtomicReference<>();
    private final AtomicReference<String> key = new AtomicReference<>();
    private final AtomicReference<String> sent = new AtomicReference<>();

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private HttpIdentityClient start(int status, String body) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            method.set(exchange.getRequestMethod());
            path.set(exchange.getRequestURI().getPath());
            key.set(exchange.getRequestHeaders().getFirst("X-Internal-Key"));
            sent.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
        server.start();
        AppProperties props = new AppProperties();
        props.getIdentity().setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        props.getInternal().setServiceKey("k");
        return new HttpIdentityClient(props);
    }

    private static String statusJson(UUID id, boolean companyLocked) {
        return "{\"userId\":\"" + id + "\",\"email\":\"hr@a.test\",\"fullName\":\"HR\",\"role\":\"HR\","
                + "\"companyId\":\"" + UUID.randomUUID() + "\",\"status\":\""
                + (companyLocked ? "LOCKED" : "ACTIVE") + "\",\"lockedReason\":null,\"lockedAt\":null,"
                + "\"tempLockedUntil\":null,\"companyLocked\":" + companyLocked + "}";
    }

    @Test
    void postsIdsAndFlagWithKey() throws Exception {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        HttpIdentityClient client = start(200, "[" + statusJson(a, true) + "," + statusJson(b, true) + "]");

        client.setCompanyLock(List.of(a, b), true);

        assertThat(method.get()).isEqualTo("POST");
        assertThat(path.get()).isEqualTo("/internal/users/company-lock");
        assertThat(key.get()).isEqualTo("k");
        JsonNode json = JSON.readTree(sent.get());
        assertThat(json.get("userIds").get(0).asText()).isEqualTo(a.toString());
        assertThat(json.get("userIds").get(1).asText()).isEqualTo(b.toString());
        assertThat(json.get("locked").isBoolean()).isTrue();
        assertThat(json.get("locked").asBoolean()).isTrue();
    }

    @Test
    void parsesCompanyLocked() throws Exception {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        HttpIdentityClient client = start(200, "[" + statusJson(a, true) + "," + statusJson(b, false) + "]");

        List<IdentityUserStatus> result = client.setCompanyLock(List.of(a, b), false);

        assertThat(JSON.readTree(sent.get()).get("locked").asBoolean()).isFalse();
        assertThat(result).extracting(IdentityUserStatus::userId).containsExactly(a, b);
        assertThat(result).extracting(IdentityUserStatus::companyLocked).containsExactly(true, false);
        assertThat(result.get(0).status()).isEqualTo("LOCKED");
    }

    @Test
    void serverErrorBecomesExternalUnavailable() throws Exception {
        HttpIdentityClient client = start(500, "{}");

        assertThatThrownBy(() -> client.setCompanyLock(List.of(UUID.randomUUID()), true))
                .isInstanceOfSatisfying(CompanyException.class,
                        e -> assertThat(e.getCode()).isEqualTo(CompanyErrorCode.EXTERNAL_UNAVAILABLE))
                .hasMessage(MESSAGE);
    }

    @Test
    void unauthorizedBecomesExternalUnavailable() throws Exception {
        HttpIdentityClient client = start(401, "{\"code\":\"INVALID_SERVICE_KEY\"}");

        assertThatThrownBy(() -> client.setCompanyLock(List.of(UUID.randomUUID()), true))
                .isInstanceOfSatisfying(CompanyException.class,
                        e -> assertThat(e.getCode()).isEqualTo(CompanyErrorCode.EXTERNAL_UNAVAILABLE))
                .hasMessage(MESSAGE);
    }

    @Test
    void connectionRefusedBecomesExternalUnavailable() throws Exception {
        HttpIdentityClient client = start(200, "[]");
        server.stop(0);

        assertThatThrownBy(() -> client.setCompanyLock(List.of(UUID.randomUUID()), false))
                .isInstanceOfSatisfying(CompanyException.class,
                        e -> assertThat(e.getCode()).isEqualTo(CompanyErrorCode.EXTERNAL_UNAVAILABLE))
                .hasMessage(MESSAGE);
    }
}
