package com.rikkei.jobs.company.config.security;

import com.rikkei.jobs.common.config.security.config.AccessDeniedExceptionHandler;
import com.rikkei.jobs.common.config.security.config.AuthenticationEntryPointHandler;
import com.rikkei.jobs.common.config.security.config.SecurityConfig;
import com.rikkei.jobs.common.config.security.jwt.JwtAuthenticationFilter;
import com.rikkei.jobs.common.config.security.jwt.JwtUtils;
import com.rikkei.jobs.company.config.AppProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
 * Chain riêng của company-service: giống chain mặc định của common-core, thêm đường công khai để
 * trình duyệt tải logo (thẻ img không gửi được Authorization). Chain mặc định nhường chỗ cho bean này.
 */
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class CompanySecurityConfig {

    @Bean
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
                        .requestMatchers(HttpMethod.GET, "/companies/logos/**").permitAll()
                        .requestMatchers(SecurityConfig.API_DOCS_PATHS).permitAll()
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(new JwtAuthenticationFilter(jwtUtils), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
