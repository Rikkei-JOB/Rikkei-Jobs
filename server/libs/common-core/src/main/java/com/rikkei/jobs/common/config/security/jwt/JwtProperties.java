package com.rikkei.jobs.common.config.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Khoá phải trùng identity-service (cùng JWT_SECRET, HS256). */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secret;
    private long expirationMs = 8 * 60 * 60 * 1000L;
}
