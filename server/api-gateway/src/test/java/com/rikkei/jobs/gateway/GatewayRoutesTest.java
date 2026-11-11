package com.rikkei.jobs.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;

@SpringBootTest
class GatewayRoutesTest {

    @Autowired RouteLocator routes;

    @Test
    void routesEveryServiceUnderApiPrefix() {
        List<String> ids = routes.getRoutes().map(r -> r.getId()).collectList().block();

        assertThat(ids)
                .containsExactlyInAnyOrder(
                        "identity-auth", "profile", "recruitment", "learning", "company", "internship", "assessment", "ai");
    }
}
