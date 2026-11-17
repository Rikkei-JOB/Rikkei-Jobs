package com.rikkei.jobs.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rikkei.jobs.company.client.HttpSkillCatalogClient;
import com.rikkei.jobs.company.config.AppProperties;
import com.rikkei.jobs.company.config.exception.CompanyErrorCode;
import com.rikkei.jobs.company.config.exception.CompanyException;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class HttpSkillCatalogClientTest {

    private static final String MESSAGE = "Không kiểm tra được danh mục kỹ năng, vui lòng thử lại.";

    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private HttpSkillCatalogClient start(int status, String body, AtomicReference<String> authSeen,
                                         AtomicReference<String> pathSeen) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            if (authSeen != null) {
                authSeen.set(exchange.getRequestHeaders().getFirst("Authorization"));
            }
            if (pathSeen != null) {
                pathSeen.set(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
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
        props.getProfile().setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        return new HttpSkillCatalogClient(props);
    }

    private static void assertUnavailable(Throwable e) {
        assertThat(e).isInstanceOfSatisfying(CompanyException.class, ex -> {
            assertThat(ex.getCode()).isEqualTo(CompanyErrorCode.EXTERNAL_UNAVAILABLE);
            assertThat(ex.getMessage()).isEqualTo(MESSAGE);
        });
    }

    @Test
    void forwardsAuthorizationAndParsesIds() throws Exception {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        String body = "[{\"id\":\"" + a + "\",\"code\":\"JAVA\",\"name\":\"Java\",\"group\":\"BACKEND\"},"
                + "{\"id\":\"" + b + "\",\"code\":\"SQL\",\"name\":\"SQL\",\"group\":\"DATA\"}]";
        AtomicReference<String> auth = new AtomicReference<>();
        AtomicReference<String> path = new AtomicReference<>();
        HttpSkillCatalogClient client = start(200, body, auth, path);

        Set<UUID> ids = client.skillIds("Bearer abc.def");

        assertThat(auth.get()).isEqualTo("Bearer abc.def");
        assertThat(path.get()).isEqualTo("GET /skills");
        assertThat(ids).containsExactlyInAnyOrder(a, b);
    }

    @Test
    void serverErrorBecomesExternalUnavailable() throws Exception {
        HttpSkillCatalogClient client = start(500, "{}", null, null);

        assertThatThrownBy(() -> client.skillIds("Bearer x")).satisfies(HttpSkillCatalogClientTest::assertUnavailable);
    }

    @Test
    void unauthorizedBecomesExternalUnavailable() throws Exception {
        HttpSkillCatalogClient client = start(401, "{}", null, null);

        assertThatThrownBy(() -> client.skillIds("Bearer x")).satisfies(HttpSkillCatalogClientTest::assertUnavailable);
    }

    @Test
    void emptyBodyBecomesExternalUnavailable() throws Exception {
        HttpSkillCatalogClient client = start(200, "", null, null);

        assertThatThrownBy(() -> client.skillIds("Bearer x")).satisfies(HttpSkillCatalogClientTest::assertUnavailable);
    }

    @Test
    void connectionRefusedBecomesExternalUnavailable() throws Exception {
        HttpSkillCatalogClient client = start(200, "[]", null, null);
        server.stop(0);

        assertThatThrownBy(() -> client.skillIds("Bearer x")).satisfies(HttpSkillCatalogClientTest::assertUnavailable);
    }
}
