package com.rikkei.jobs.recruitment.config;

import java.time.Clock;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class RecruitmentConfiguration {
    @Bean public Clock recruitmentClock() { return Clock.systemUTC(); }
}
