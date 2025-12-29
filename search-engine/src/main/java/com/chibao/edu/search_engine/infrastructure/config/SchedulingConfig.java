package com.chibao.edu.search_engine.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration to enable scheduled tasks.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Enabled Spring's @Scheduled annotation support
}
