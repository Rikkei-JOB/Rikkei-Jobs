package com.rikkei.jobs.company.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Đồng hồ hệ thống dùng chung; tiêm vào service để test có thể thay bằng đồng hồ cố định. */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
