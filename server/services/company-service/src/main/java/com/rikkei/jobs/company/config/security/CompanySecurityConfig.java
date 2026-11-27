package com.rikkei.jobs.company.config.security;

import com.rikkei.jobs.common.config.security.config.AccessDeniedExceptionHandler;
import com.rikkei.jobs.common.config.security.config.AuthenticationEntryPointHandler;
import com.rikkei.jobs.common.config.security.config.SecurityConfig;
import com.rikkei.jobs.common.config.security.jwt.JwtAuthenticationFilter;
import com.rikkei.jobs.common.config.security.jwt.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.company.config.AppProperties;
import com.rikkei.jobs.company.repository.CompanyHrRepository;
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
                                           CompanyHrRepository companyHrRepository,
                                           ObjectMapper objectMapper,
                                           AppProperties appProperties,
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
                        // Bảng việc làm công khai (PublicJobController): khách chưa đăng nhập cũng xem được.
                        .requestMatchers(HttpMethod.GET, "/public/jobs", "/public/jobs/*", "/public/companies",
                                "/public/companies/*").permitAll()
                        .requestMatchers(SecurityConfig.API_DOCS_PATHS).permitAll()
                        // /internal/** do InternalKeyFilter bảo vệ, JWT không áp dụng.
                        .requestMatchers(InternalKeyFilter.INTERNAL_PATTERN).permitAll()
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(new JwtAuthenticationFilter(jwtUtils), UsernamePasswordAuthenticationFilter.class);
        // Không khai báo @Component để tránh đăng ký hai lần; đặt trước JWT để /internal/** không cần token.
        http.addFilterBefore(new InternalKeyFilter(appProperties.getInternal().getServiceKey(), objectMapper),
                JwtAuthenticationFilter.class);
        // HR bị cán bộ khoá bị chặn ngay cả khi JWT còn hạn.
        http.addFilterAfter(new HrLockedFilter(companyHrRepository, objectMapper), JwtAuthenticationFilter.class);

        return http.build();
    }
}
