package com.rikkei.jobs.common.config.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.config.security.jwt.JwtAuthenticationFilter;
import com.rikkei.jobs.common.config.security.jwt.JwtProperties;
import com.rikkei.jobs.common.config.security.jwt.JwtUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Cấu hình bảo mật mặc định cho mọi service nghiệp vụ: stateless + JWT do identity-service cấp.
 * Service nào cần luật riêng thì tự khai báo bean {@link SecurityFilterChain}, bean ở đây sẽ nhường chỗ.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    /** Tài liệu OpenAPI của service ({@code /<khu>/api-docs}), để Swagger UI ở gateway đọc được mà không cần JWT. */
    public static final String[] API_DOCS_PATHS = {"/*/api-docs", "/*/api-docs/**"};

    @Bean
    @ConditionalOnMissingBean
    public JwtUtils jwtUtils(JwtProperties jwtProperties) {
        return new JwtUtils(jwtProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthenticationEntryPointHandler authenticationEntryPointHandler(ObjectMapper objectMapper) {
        return new AuthenticationEntryPointHandler(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public AccessDeniedExceptionHandler accessDeniedExceptionHandler(ObjectMapper objectMapper) {
        return new AccessDeniedExceptionHandler(objectMapper);
    }

    /** Người dùng do identity-service quản lý; bean này chỉ để Spring không tạo user mặc định. */
    @Bean
    @ConditionalOnMissingBean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Người dùng được quản lý bởi identity-service");
        };
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtUtils jwtUtils,
                                           AuthenticationEntryPointHandler authenticationEntryPointHandler,
                                           AccessDeniedExceptionHandler accessDeniedExceptionHandler) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPointHandler)
                        .accessDeniedHandler(accessDeniedExceptionHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers(API_DOCS_PATHS).permitAll()
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(new JwtAuthenticationFilter(jwtUtils), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** Trình duyệt thường đi qua gateway; cấu hình này chỉ cần khi gọi thẳng vào service. */
    @Bean
    @ConditionalOnMissingBean(CorsConfigurationSource.class)
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
