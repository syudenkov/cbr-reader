package com.cbrviewer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class CacheConfig {
    // @EnableScheduling activates @Scheduled methods in the application
}
