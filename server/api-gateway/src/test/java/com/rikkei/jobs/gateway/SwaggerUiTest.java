package com.rikkei.jobs.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class SwaggerUiTest {

    @Autowired WebTestClient client;

    @Test
    void swaggerUiIsServedAtApiDocs() {
        client.get().uri("/api/docs").exchange().expectStatus().is3xxRedirection();
    }

    @Test
    void swaggerUiListsEveryServiceThroughTheApiPrefix() {
        client.get().uri("/api/docs/gateway-api-docs/swagger-config").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.urls.length()").isEqualTo(8)
                .jsonPath("$.urls[?(@.name == 'identity-service')].url").isEqualTo("/api/v1/auth/api-docs")
                .jsonPath("$.urls[?(@.name == 'company-service')].url").isEqualTo("/api/v1/company/api-docs");
    }
}
