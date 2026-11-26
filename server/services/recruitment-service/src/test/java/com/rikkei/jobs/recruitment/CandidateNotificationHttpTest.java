package com.rikkei.jobs.recruitment;

import com.rikkei.jobs.recruitment.entity.ApplicationStatus;
import com.rikkei.jobs.recruitment.service.ApplicationTransitionService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "app.recruitment.outbox-enabled=false", "app.recruitment.interview-jobs-enabled=false",
    "app.jwt.secret=test_secret_test_secret_test_secret_test_secret", "app.recruitment.cv-storage-dir=target/ut01-snapshots"
})
class CandidateNotificationHttpTest extends ApplicationIntegrationSupport {
    @LocalServerPort int port;
    @Autowired ApplicationTransitionService transitions;

    @Test void realHttpClientReadsAndMarksAPersistedBusinessEvent() throws Exception {
        UUID application = apply();
        transitions.transition(application, ApplicationStatus.SCREENING, UUID.randomUUID(), null);
        var client = HttpClient.newHttpClient();
        String root = "http://127.0.0.1:" + port + "/recruitment/me/notifications";
        var response = client.send(HttpRequest.newBuilder(URI.create(root)).header("Authorization", candidate()).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        var data = json.readTree(response.body());
        assertEquals(1, data.get("unreadCount").asInt());
        assertEquals("/candidate/applications/" + application, data.get("items").get(0).get("href").asText());
        String id = data.get("items").get(0).get("id").asText();
        var marked = client.send(HttpRequest.newBuilder(URI.create(root + "/" + id + "/read")).header("Authorization", candidate()).POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.discarding());
        assertEquals(204, marked.statusCode());
        var after = client.send(HttpRequest.newBuilder(URI.create(root)).header("Authorization", candidate()).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(0, json.readTree(after.body()).get("unreadCount").asInt());
        assertNotNull(json.readTree(after.body()).get("items").get(0).get("readAt").textValue());
        assertEquals(401, client.send(HttpRequest.newBuilder(URI.create(root)).GET().build(), HttpResponse.BodyHandlers.discarding()).statusCode());
    }
}
