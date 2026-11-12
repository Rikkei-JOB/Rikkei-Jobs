package com.rikkei.jobs.company.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** JWT (app.jwt.*) do JwtProperties của common-core đọc. */
@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Identity identity = new Identity();
    private Internal internal = new Internal();
    private Upload upload = new Upload();
    private DevData devData = new DevData();

    @Getter
    @Setter
    public static class Identity {
        private String baseUrl = "http://localhost:8081";
    }

    @Getter
    @Setter
    public static class Internal {
        /** Khoá X-Internal-Key khi gọi /internal/** của identity-service. */
        private String serviceKey = "";
    }

    @Getter
    @Setter
    public static class Upload {
        private String dir = "./data/uploads";
    }

    @Getter
    @Setter
    public static class DevData {
        private boolean enabled = false;
    }
}
