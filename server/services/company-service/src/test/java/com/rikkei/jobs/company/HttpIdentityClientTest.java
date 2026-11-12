package com.rikkei.jobs.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.config.exception.BadRequestException;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.company.client.CreatedUser;
import com.rikkei.jobs.company.client.EmailExistsException;
import com.rikkei.jobs.company.client.HttpIdentityClient;
import com.rikkei.jobs.company.config.AppProperties;
import com.rikkei.jobs.company.config.exception.CompanyErrorCode;
import com.rikkei.jobs.company.config.exception.CompanyException;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class HttpIdentityClientTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private HttpIdentityClient start(int status, String body, AtomicReference<String> keySeen,
                                     AtomicReference<String> bodySeen) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            if (keySeen != null) {
                keySeen.set(exchange.getRequestHeaders().getFirst("X-Internal-Key"));
            }
            if (bodySeen != null) {
                bodySeen.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            }
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
        props.getIdentity().setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        props.getInternal().setServiceKey("k");
        return new HttpIdentityClient(props);
    }

    @Test
    void sendsKeyAndParsesCreated() throws Exception {
        UUID uid = UUID.randomUUID();
        UUID cid = UUID.randomUUID();
        AtomicReference<String> key = new AtomicReference<>();
        AtomicReference<String> sent = new AtomicReference<>();
        String body = "{\"userId\":\"" + uid + "\",\"email\":\"a@b.test\",\"fullName\":\"A\",\"role\":\"HR\","
                + "\"companyId\":\"" + cid + "\"}";
        HttpIdentityClient client = start(201, body, key, sent);

        CreatedUser created = client.createHrUser("a@b.test", "A", cid);

        assertThat(key.get()).isEqualTo("k");
        JsonNode json = JSON.readTree(sent.get());
        assertThat(json.get("role").asText()).isEqualTo("HR");
        assertThat(json.get("companyId").asText()).isEqualTo(cid.toString());
        assertThat(created).isEqualTo(new CreatedUser(uid, "a@b.test", "A", cid));
    }

    @Test
    void conflictBecomesEmailExists() throws Exception {
        UUID uid = UUID.randomUUID();
        UUID cid = UUID.randomUUID();
        HttpIdentityClient client = start(409, "{\"code\":\"EMAIL_EXISTS\",\"message\":\"..\",\"errors\":{\"userId\":\""
                + uid + "\",\"role\":\"HR\",\"companyId\":\"" + cid + "\"}}", null, null);

        assertThatThrownBy(() -> client.createHrUser("a@b.test", "A", cid))
                .isInstanceOfSatisfying(EmailExistsException.class, e -> {
                    assertThat(e.userId()).isEqualTo(uid);
                    assertThat(e.role()).isEqualTo(Role.HR);
                    assertThat(e.companyId()).isEqualTo(cid);
                });
    }

    @Test
    void conflictWithoutCompanyIdKeepsItNull() throws Exception {
        UUID uid = UUID.randomUUID();
        HttpIdentityClient client = start(409, "{\"code\":\"EMAIL_EXISTS\",\"message\":\"..\",\"errors\":{\"userId\":\""
                + uid + "\",\"role\":\"CANDIDATE\"}}", null, null);

        assertThatThrownBy(() -> client.createHrUser("a@b.test", "A", UUID.randomUUID()))
                .isInstanceOfSatisfying(EmailExistsException.class, e -> {
                    assertThat(e.role()).isEqualTo(Role.CANDIDATE);
                    assertThat(e.companyId()).isNull();
                });
    }

    @Test
    void badRequestKeepsIdentityMessage() throws Exception {
        HttpIdentityClient client = start(400, "{\"code\":\"VALIDATION_ERROR\",\"message\":\"Email sai\"}", null, null);

        assertThatThrownBy(() -> client.createHrUser("x", "A", UUID.randomUUID()))
                .isInstanceOf(BadRequestException.class).hasMessage("Email sai");
    }

    @Test
    void serverErrorBecomesExternalUnavailable() throws Exception {
        HttpIdentityClient client = start(500, "{}", null, null);

        assertThatThrownBy(() -> client.createHrUser("a@b.test", "A", UUID.randomUUID()))
                .isInstanceOfSatisfying(CompanyException.class,
                        e -> assertThat(e.getCode()).isEqualTo(CompanyErrorCode.EXTERNAL_UNAVAILABLE));
    }

    @Test
    void connectionRefusedBecomesExternalUnavailable() throws Exception {
        HttpIdentityClient client = start(201, "{}", null, null);
        server.stop(0);

        assertThatThrownBy(() -> client.createHrUser("a@b.test", "A", UUID.randomUUID()))
                .isInstanceOfSatisfying(CompanyException.class,
                        e -> assertThat(e.getCode()).isEqualTo(CompanyErrorCode.EXTERNAL_UNAVAILABLE));
    }

    @Test
    void deleteTreats404AsDone() throws Exception {
        HttpIdentityClient client = start(404, "", null, null);

        assertThatCode(() -> client.deleteUser(UUID.randomUUID())).doesNotThrowAnyException();
    }

    @Test
    void deleteServerErrorBecomesExternalUnavailable() throws Exception {
        HttpIdentityClient client = start(500, "{}", null, null);

        assertThatThrownBy(() -> client.deleteUser(UUID.randomUUID())).isInstanceOf(CompanyException.class);
    }
}
