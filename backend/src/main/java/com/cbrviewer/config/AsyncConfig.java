package com.cbrviewer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous TTS job processing.
 * Configures single-threaded executor to ensure serial job execution.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    private static final Logger logger = LoggerFactory.getLogger(AsyncConfig.class);

    /**
     * Creates ThreadPoolTaskExecutor for TTS job processing.
     * Single-threaded (corePoolSize=1, maxPoolSize=1) ensures only one job runs at a time.
     *
     * @return configured Executor for @Async methods
     */
    @Bean(name = "ttsTaskExecutor")
    public Executor ttsTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);           // Single thread for serial processing
        executor.setMaxPoolSize(1);            // No scaling beyond 1 thread
        executor.setQueueCapacity(100);        // Up to 100 jobs in queue
        executor.setThreadNamePrefix("tts-"); // Thread name prefix for logging
        executor.initialize();

        logger.info("TTS TaskExecutor configured: corePoolSize=1, maxPoolSize=1, queueCapacity=100");
        return executor;
    }
}
