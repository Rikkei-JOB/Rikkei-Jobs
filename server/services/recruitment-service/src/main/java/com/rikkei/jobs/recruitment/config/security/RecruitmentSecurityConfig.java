package com.rikkei.jobs.recruitment.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.config.security.config.AccessDeniedExceptionHandler;
import com.rikkei.jobs.common.config.security.config.AuthenticationEntryPointHandler;
import com.rikkei.jobs.common.config.security.config.SecurityConfig;
import com.rikkei.jobs.common.config.security.jwt.JwtAuthenticationFilter;
import com.rikkei.jobs.common.config.security.jwt.JwtUtils;
import com.rikkei.jobs.recruitment.client.HrAccountStatusClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class RecruitmentSecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtUtils jwtUtils, HrAccountStatusClient accounts,
        ObjectMapper json, AuthenticationEntryPointHandler entry, AccessDeniedExceptionHandler denied) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(errors -> errors.authenticationEntryPoint(entry).accessDeniedHandler(denied))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                .requestMatchers(SecurityConfig.API_DOCS_PATHS).permitAll()
                .anyRequest().authenticated());
        http.addFilterBefore(new JwtAuthenticationFilter(jwtUtils), UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(new HrLockedFilter(accounts, json), JwtAuthenticationFilter.class);
        return http.build();
    }
}
