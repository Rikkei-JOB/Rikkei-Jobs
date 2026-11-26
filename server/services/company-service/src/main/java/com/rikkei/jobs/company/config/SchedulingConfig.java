package com.rikkei.jobs.company.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Bật @Scheduled cho các tác vụ định kỳ (xem JobAutoCloseScheduler). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
