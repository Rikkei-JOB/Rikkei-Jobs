package com.rikkei.jobs.identity.config.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.config.security.config.AccessDeniedExceptionHandler;
import com.rikkei.jobs.common.config.security.config.AuthenticationEntryPointHandler;
import com.rikkei.jobs.common.config.security.jwt.JwtUtils;
import com.rikkei.jobs.identity.config.AppProperties;
import com.rikkei.jobs.identity.config.security.InternalKeyFilter;
import com.rikkei.jobs.identity.config.security.jwt.IdentityJwtAuthenticationFilter;
import com.rikkei.jobs.identity.repository.UserRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Clock;
import java.util.List;

/**
 * Chain riêng của identity-service (có kiểm tra must_change_password theo DB), nên chain mặc định của
 * common-core tự nhường chỗ. JWT, entry point và access-denied handler vẫn dùng chung từ common-core.
 */
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class IdentitySecurityConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           AppProperties properties,
                                           JwtUtils jwtUtils,
                                           UserRepository userRepository,
                                           ObjectMapper objectMapper,
                                           CorsConfigurationSource corsConfigurationSource,
                                           AuthenticationEntryPointHandler authenticationEntryPointHandler,
                                           AccessDeniedExceptionHandler accessDeniedExceptionHandler) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPointHandler)
                        .accessDeniedHandler(accessDeniedExceptionHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/auth/sso", "/auth/login", "/auth/forgot-password", "/auth/reset-password",
                                "/auth/dev/sso-ticket").permitAll()
                        // /internal/** không dùng JWT: InternalKeyFilter kiểm tra X-Internal-Key.
                        .requestMatchers("/internal/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(
                new InternalKeyFilter(properties.getInternal().getServiceKey(), objectMapper),
                UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(
                new IdentityJwtAuthenticationFilter(jwtUtils, userRepository, objectMapper),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(AppProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(properties.getWeb().getBaseUrl()));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
