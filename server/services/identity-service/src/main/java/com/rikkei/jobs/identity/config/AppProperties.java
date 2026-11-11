package com.rikkei.jobs.identity.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** JWT (app.jwt.*) do JwtProperties của common-core đọc. */
@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Auth auth = new Auth();
    private Web web = new Web();
    private Mail mail = new Mail();
    private Lms lms = new Lms();

    @Getter
    @Setter
    public static class Auth {
        private int maxFailedAttempts = 5;
        private int lockMinutes = 15;
        private long ssoTicketTtlSeconds = 60;
        private long resetTokenTtlMinutes = 30;
    }

    @Getter
    @Setter
    public static class Web {
        private String baseUrl = "http://localhost:3000";
    }

    @Getter
    @Setter
    public static class Mail {
        private boolean enabled = true;
        private String from = "no-reply@rikkei-jobs.local";
    }

    @Getter
    @Setter
    public static class DevTickets {
        private boolean enabled = false;
    }

    @Getter
    @Setter
    public static class Lms {
        private String baseUrl = "http://localhost:9000";
        private String serviceKey = "dev-key";
        private DevTickets devTickets = new DevTickets();
    }
}
