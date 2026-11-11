package com.rikkei.jobs.profile.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class SkillProfileConfiguration {
    @Bean
    public Clock skillProfileClock() {
        return Clock.systemUTC();
    }
}
