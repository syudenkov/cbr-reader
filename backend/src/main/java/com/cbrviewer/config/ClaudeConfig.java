package com.cbrviewer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class for Claude client dependencies.
 * Provides RestTemplate bean with timeout configuration for Claude API calls.
 */
@Configuration
public class ClaudeConfig {

    private static final Logger logger = LoggerFactory.getLogger(ClaudeConfig.class);

    /**
     * Creates a RestTemplate configured with 30-second timeout for Claude API calls.
     * Timeout matches OpenAI client for consistency.
     *
     * @return configured RestTemplate instance
     */
    @Bean
    public RestTemplate claudeRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000); // 30 seconds in milliseconds
        factory.setReadTimeout(30000);    // 30 seconds in milliseconds

        RestTemplate restTemplate = new RestTemplate(factory);
        logger.info("Claude RestTemplate configured with 30-second timeout");
        return restTemplate;
    }
}
