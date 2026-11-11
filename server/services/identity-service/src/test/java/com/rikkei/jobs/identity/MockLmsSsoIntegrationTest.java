package com.rikkei.jobs.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.identity.repository.UserRepository;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Chạy qua HTTP thật tới một server giả lập đúng hợp đồng của mock/ (mock/server.js): kiểm tra X-Service-Key,
 * POST /lms/sso/tickets → 201, POST /lms/sso/verify → 200 hoặc 404 NOT_FOUND, và KHÔNG chặn vé cũ/đã dùng.
 * Nhờ vậy kiểm tra được phần backend phải tự làm: hết hạn 60 giây, dùng một lần, lỗi khi mock tắt hoặc sai khoá.
 */
@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:identity-lms;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "app.dev-data.enabled=true",
            "app.lms.dev-tickets.enabled=true",
            "app.lms.service-key=dev-key",
            "app.mail.enabled=false"
        })
@AutoConfigureMockMvc
class MockLmsSsoIntegrationTest {

    private static final Map<String, String[]> USERS = Map.of(
            "lms-sv-001", new String[] {"candidate1@rikkeijobs.test", "Nguyễn Văn An", "CANDIDATE"},
            "lms-mt-001", new String[] {"mentor1@rikkeijobs.test", "Phạm Quốc Dũng", "MENTOR"},
            "lms-ad-001", new String[] {"admin1@rikkeijobs.test", "Đỗ Thanh Hải", "ADMIN"});

    private static final Map<String, Object[]> TICKETS = new ConcurrentHashMap<>(); // ticket -> {lmsUserId, issuedAt}
    private static final List<Boolean> KEY_SEEN = new CopyOnWriteArrayList<>();
    private static final AtomicInteger SEQ = new AtomicInteger();
    private static final ObjectMapper JSON = new ObjectMapper();
    private static HttpServer lms;

    @BeforeAll
    static void startFakeLms() throws IOException {
        lms = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        lms.createContext("/lms/sso/tickets", ex -> {
            if (!keyOk(ex)) {
                reply(ex, 401, "{\"code\":\"INVALID_SERVICE_KEY\",\"message\":\"Thiếu hoặc sai X-Service-Key\"}");
                return;
            }
            String id = JSON.readTree(ex.getRequestBody()).path("lmsUserId").asText();
            if (!USERS.containsKey(id)) {
                reply(ex, 404, "{\"code\":\"NOT_FOUND\",\"message\":\"lmsUserId không tồn tại\"}");
                return;
            }
            String ticket = "tk-" + SEQ.incrementAndGet();
            Instant issuedAt = Instant.now();
            TICKETS.put(ticket, new Object[] {id, issuedAt});
            reply(ex, 201, "{\"ticket\":\"" + ticket + "\",\"issuedAt\":\"" + issuedAt + "\"}");
        });
        lms.createContext("/lms/sso/verify", ex -> {
            if (!keyOk(ex)) {
                reply(ex, 401, "{\"code\":\"INVALID_SERVICE_KEY\",\"message\":\"Thiếu hoặc sai X-Service-Key\"}");
                return;
            }
            Object[] t = TICKETS.get(JSON.readTree(ex.getRequestBody()).path("ticket").asText());
            if (t == null) {
                reply(ex, 404, "{\"code\":\"NOT_FOUND\",\"message\":\"ticket không tồn tại\"}");
                return;
            }
            String[] u = USERS.get((String) t[0]);
            reply(ex, 200, JSON.writeValueAsString(Map.of(
                    "lmsUserId", t[0], "email", u[0], "fullName", u[1], "role", u[2], "issuedAt", t[1].toString())));
        });
        lms.start();
    }

    @AfterAll
    static void stopFakeLms() {
        lms.stop(0);
    }

    @DynamicPropertySource
    static void lmsUrl(DynamicPropertyRegistry registry) throws IOException {
        // Server được tạo trong @BeforeAll nhưng property cần có trước khi context khởi động, nên tạo sớm ở đây.
        if (lms == null) {
            startFakeLms();
        }
        registry.add("app.lms.base-url", () -> "http://localhost:" + lms.getAddress().getPort());
    }

    private static boolean keyOk(com.sun.net.httpserver.HttpExchange ex) {
        boolean ok = "dev-key".equals(ex.getRequestHeaders().getFirst("X-Service-Key"));
        KEY_SEEN.add(ok);
        return ok;
    }

    private static void reply(com.sun.net.httpserver.HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;

    private String devTicket(String lmsUserId) throws Exception {
        String body = mvc.perform(post("/auth/dev/sso-ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lmsUserId\":\"" + lmsUserId + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode node = JSON.readTree(body);
        return node.get("ticket").asText();
    }

    private ResultActions sso(String ticket) throws Exception {
        return mvc.perform(post("/auth/sso")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ticket\":\"" + ticket + "\"}"));
    }

    @Test
    void seededCandidate1LogsInViaTicketOnlyOnceAndKeyIsSent() throws Exception {
        long before = users.count();
        String ticket = devTicket("lms-sv-001");

        String body = sso(ticket)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("CANDIDATE"))
                .andExpect(jsonPath("$.user.email").value("candidate1@rikkeijobs.test"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = JSON.readTree(body).get("accessToken").asText();

        mvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CANDIDATE"));
        // Tài khoản đã seed sẵn nên SSO dùng lại, không tạo thêm.
        assertThat(users.count()).isEqualTo(before);
        assertThat(KEY_SEEN).doesNotContain(false);

        // Mock vẫn cho verify lại; backend phải tự chặn vé đã dùng.
        sso(ticket).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("TICKET_ALREADY_USED"));
    }

    @Test
    void ticketOlderThan60SecondsIsRejected() throws Exception {
        String ticket = devTicket("lms-mt-001");
        TICKETS.get(ticket)[1] = Instant.now().minusSeconds(61);

        sso(ticket).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("TICKET_EXPIRED"));
    }

    @Test
    void roleAndNameComeFromLms() throws Exception {
        sso(devTicket("lms-ad-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.user.fullName").value("Đỗ Thanh Hải"));
    }

    @Test
    void ticketUnknownToLmsIsRejectedAs401() throws Exception {
        sso("khong-co-trong-lms").andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_TICKET"));
    }

    @Test
    void devTicketForUnknownLmsUserIsRejected() throws Exception {
        mvc.perform(post("/auth/dev/sso-ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lmsUserId\":\"lms-sv-999\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TICKET"));
    }

    @Test
    void seededSampleAccountsMatchConventions() {
        assertThat(users.findByEmail("hr1@rikkeijobs.test")).isPresent();
        assertThat(users.findByEmail("hr2@rikkeijobs.test")).isPresent();
        assertThat(users.findByEmail("hr1@rikkeijobs.test").get().getCompanyId())
                .isNotEqualTo(users.findByEmail("hr2@rikkeijobs.test").get().getCompanyId());
        assertThat(users.findByLmsUserId("lms-sv-002")).isPresent();
        assertThat(users.findByLmsUserId("lms-st-001").get().getEmail()).isEqualTo("staff1@rikkeijobs.test");
        assertThat(users.findByEmail("hr-new@rikkeijobs.test").get().getMustChangePassword()).isTrue();
    }
}
