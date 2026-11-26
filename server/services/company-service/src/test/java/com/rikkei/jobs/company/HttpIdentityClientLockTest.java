package com.rikkei.jobs.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.config.exception.BadRequestException;
import com.rikkei.jobs.common.config.exception.ConflictException;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.company.client.HttpIdentityClient;
import com.rikkei.jobs.company.client.IdentityUserStatus;
import com.rikkei.jobs.company.config.AppProperties;
import com.rikkei.jobs.company.config.exception.CompanyErrorCode;
import com.rikkei.jobs.company.config.exception.CompanyException;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class HttpIdentityClientLockTest {

    private static final ObjectMapper JSON = new ObjectMapper();

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

    private static String statusJson(UUID id, String status) {
        return "{\"userId\":\"" + id + "\",\"email\":\"hr@b.test\",\"fullName\":\"HR\",\"role\":\"HR\","
                + "\"companyId\":\"" + UUID.randomUUID() + "\",\"status\":\"" + status + "\","
                + "\"lockedReason\":\"Vi phạm\",\"lockedAt\":\"2026-11-17T03:00:00Z\","
                + "\"tempLockedUntil\":\"2026-11-18T03:00:00Z\"}";
    }

    @Test
    void lockSendsPatchWithReasonAndKey() throws Exception {
        UUID id = UUID.randomUUID();
        HttpIdentityClient client = start(200, statusJson(id, "LOCKED"));

        client.lockUser(id, "Vi phạm");

        assertThat(method.get()).isEqualTo("PATCH");
        assertThat(path.get()).isEqualTo("/internal/users/" + id + "/lock");
        assertThat(key.get()).isEqualTo("k");
        JsonNode json = JSON.readTree(sent.get());
        assertThat(json.get("reason").asText()).isEqualTo("Vi phạm");
    }

    @Test
    void lockParsesStatus() throws Exception {
        UUID id = UUID.randomUUID();
        HttpIdentityClient client = start(200, statusJson(id, "LOCKED"));

        IdentityUserStatus status = client.lockUser(id, "Vi phạm");

        assertThat(status).isEqualTo(new IdentityUserStatus(id, "hr@b.test", "HR", "LOCKED", "Vi phạm",
                Instant.parse("2026-11-17T03:00:00Z"), Instant.parse("2026-11-18T03:00:00Z"), null));
    }

    @Test
    void unlockSendsPatchAndParses() throws Exception {
        UUID id = UUID.randomUUID();
        HttpIdentityClient client = start(200,
                "{\"userId\":\"" + id + "\",\"email\":\"hr@b.test\",\"fullName\":\"HR\",\"status\":\"ACTIVE\"}");

        IdentityUserStatus status = client.unlockUser(id);

        assertThat(method.get()).isEqualTo("PATCH");
        assertThat(path.get()).isEqualTo("/internal/users/" + id + "/unlock");
        assertThat(status.status()).isEqualTo("ACTIVE");
        assertThat(status.lockedAt()).isNull();
        assertThat(status.tempLockedUntil()).isNull();
    }

    @Test
    void lookupPostsIdsAndParsesList() throws Exception {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        HttpIdentityClient client = start(200, "[" + statusJson(a, "LOCKED") + "," + statusJson(b, "ACTIVE") + "]");

        List<IdentityUserStatus> result = client.lookupUsers(List.of(a, b));

        assertThat(method.get()).isEqualTo("POST");
        assertThat(path.get()).isEqualTo("/internal/users/lookup");
        assertThat(key.get()).isEqualTo("k");
        JsonNode json = JSON.readTree(sent.get());
        assertThat(json.get("userIds").get(0).asText()).isEqualTo(a.toString());
        assertThat(json.get("userIds").get(1).asText()).isEqualTo(b.toString());
        assertThat(result).extracting(IdentityUserStatus::userId).containsExactly(a, b);
    }

    @Test
    void notFoundBecomesResourceNotFound() throws Exception {
        HttpIdentityClient client = start(404, "{\"code\":\"NOT_FOUND\",\"message\":\"Không có\"}");

        assertThatThrownBy(() -> client.lockUser(UUID.randomUUID(), "x"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void conflictBecomesConflict() throws Exception {
        HttpIdentityClient client = start(409, "{\"code\":\"CONFLICT\",\"message\":\"Không phải HR\"}");

        assertThatThrownBy(() -> client.unlockUser(UUID.randomUUID()))
                .isInstanceOf(ConflictException.class).hasMessage("Không phải HR");
    }

    @Test
    void badRequestBecomesBadRequest() throws Exception {
        HttpIdentityClient client = start(400, "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Sai\"}");

        assertThatThrownBy(() -> client.lookupUsers(List.of(UUID.randomUUID())))
                .isInstanceOf(BadRequestException.class).hasMessage("Sai");
    }

    @Test
    void unauthorizedBecomesExternalUnavailable() throws Exception {
        HttpIdentityClient client = start(401, "{\"code\":\"INVALID_SERVICE_KEY\"}");

        assertThatThrownBy(() -> client.lockUser(UUID.randomUUID(), "x"))
                .isInstanceOfSatisfying(CompanyException.class,
                        e -> assertThat(e.getCode()).isEqualTo(CompanyErrorCode.EXTERNAL_UNAVAILABLE));
    }

    @Test
    void serverErrorBecomesExternalUnavailable() throws Exception {
        HttpIdentityClient client = start(500, "{}");

        assertThatThrownBy(() -> client.lockUser(UUID.randomUUID(), "x"))
                .isInstanceOfSatisfying(CompanyException.class,
                        e -> assertThat(e.getCode()).isEqualTo(CompanyErrorCode.EXTERNAL_UNAVAILABLE));
    }

    @Test
    void connectionRefusedBecomesExternalUnavailable() throws Exception {
        HttpIdentityClient client = start(200, "{}");
        server.stop(0);

        assertThatThrownBy(() -> client.lookupUsers(List.of(UUID.randomUUID())))
                .isInstanceOfSatisfying(CompanyException.class,
                        e -> assertThat(e.getCode()).isEqualTo(CompanyErrorCode.EXTERNAL_UNAVAILABLE));
    }
}
