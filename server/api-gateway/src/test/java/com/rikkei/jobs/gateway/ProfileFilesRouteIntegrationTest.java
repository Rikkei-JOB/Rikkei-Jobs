package com.rikkei.jobs.gateway;

import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "logging.level.root=WARN")
class ProfileFilesRouteIntegrationTest {
    private static final AtomicReference<String> path = new AtomicReference<>();
    private static final AtomicReference<String> authorization = new AtomicReference<>();
    private static final AtomicReference<String> contentType = new AtomicReference<>();
    private static final AtomicReference<String> body = new AtomicReference<>();
    private static final HttpServer profile;
    static {
        try {
            profile = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            profile.createContext("/", exchange -> {
                path.set(exchange.getRequestURI().getPath());
                authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
                body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                byte[] bytes = "%PDF-1.7\nexample".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/pdf");
                exchange.sendResponseHeaders(exchange.getRequestMethod().equals("POST") ? 201 : 200, bytes.length);
                exchange.getResponseBody().write(bytes); exchange.close();
            });
            profile.start();
        } catch (Exception ex) { throw new ExceptionInInitializerError(ex); }
    }
    @LocalServerPort int port;
    @DynamicPropertySource static void address(DynamicPropertyRegistry registry) {
        registry.add("PROFILE_SERVICE_URL", () -> "http://127.0.0.1:" + profile.getAddress().getPort());
    }
    @AfterAll static void stop() { profile.stop(0); }
    @Test void forwardsPrivateFileDownloadAndMultipartWithoutChangingBody() throws Exception {
        var http = HttpClient.newHttpClient();
        var response = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/files/example"))
            .header("Authorization", "Bearer candidate").GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("%PDF-1.7\nexample");
        assertThat(path.get()).isEqualTo("/files/example");
        assertThat(authorization.get()).isEqualTo("Bearer candidate");
        String multipart = "--boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"a.pdf\"\r\n"
            + "Content-Type: application/pdf\r\n\r\n%PDF-1.7\r\n--boundary--\r\n";
        var uploaded = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/files"))
            .header("Authorization", "Bearer candidate").header("Content-Type", "multipart/form-data; boundary=boundary")
            .POST(HttpRequest.BodyPublishers.ofString(multipart)).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(uploaded.statusCode()).isEqualTo(201);
        assertThat(path.get()).isEqualTo("/files");
        assertThat(contentType.get()).isEqualTo("multipart/form-data; boundary=boundary");
        assertThat(body.get()).isEqualTo(multipart);
    }
}
