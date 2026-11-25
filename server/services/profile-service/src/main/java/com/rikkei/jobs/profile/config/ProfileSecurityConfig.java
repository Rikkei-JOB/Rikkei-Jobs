package com.rikkei.jobs.profile.config;

import com.rikkei.jobs.common.config.security.config.AccessDeniedExceptionHandler;
import com.rikkei.jobs.common.config.security.config.AuthenticationEntryPointHandler;
import com.rikkei.jobs.common.config.security.config.SecurityConfig;
import com.rikkei.jobs.common.config.security.jwt.JwtAuthenticationFilter;
import com.rikkei.jobs.common.config.security.jwt.JwtUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Chain riêng của profile-service: giống chain mặc định của common-core, thêm trang hồ sơ công khai HS-04
 * mà QR trên CV trỏ tới (người quét không đăng nhập). Chain mặc định nhường chỗ cho bean này.
 */
@Configuration
public class ProfileSecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtUtils jwtUtils,
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
                        .requestMatchers(HttpMethod.GET, "/profile/public/*").permitAll()
                        .requestMatchers(SecurityConfig.API_DOCS_PATHS).permitAll()
                        .anyRequest().authenticated()
                );
        http.addFilterBefore(new JwtAuthenticationFilter(jwtUtils), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
