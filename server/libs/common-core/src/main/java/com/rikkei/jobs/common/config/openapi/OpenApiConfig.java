package com.rikkei.jobs.common.config.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Tài liệu OpenAPI sinh từ code cho mỗi service, đọc tại {@code /<khu>/api-docs}
 * (đặt bằng {@code springdoc.api-docs.path} trong application.yml của service).
 *
 * <p>Đường dẫn trong tài liệu là đường dẫn của service ({@code /company/...}); server {@code /api/v1} khiến
 * Swagger UI ở gateway ({@code http://localhost:8080/api/docs}) gọi đúng {@code /api/v1/company/...}.
 * Nút "Authorize" nhận JWT, áp cho mọi endpoint.
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER = "bearerAuth";

    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI serviceOpenApi(@Value("${spring.application.name:service}") String applicationName) {
        return new OpenAPI()
                .info(new Info()
                        .title(applicationName)
                        .version("v1")
                        .description("Gọi qua api-gateway với tiền tố /api/v1. Lỗi có dạng {code, message, errors?}."))
                .servers(List.of(new Server().url("/api/v1").description("Qua api-gateway")))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("accessToken lấy từ POST /auth/login hoặc /auth/sso")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
