package com.cbrviewer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class for Minimax TTS client dependencies.
 * Provides RestTemplate bean with 60-second timeout for TTS API calls.
 *
 * TTS synthesis takes longer than OCR operations, hence the longer timeout
 * compared to OpenAI/Claude clients (which use 30 seconds).
 */
@Configuration
public class MinimaxConfig {

    private static final Logger logger = LoggerFactory.getLogger(MinimaxConfig.class);

    /**
     * Creates a RestTemplate configured with 60-second timeout for Minimax TTS API.
     * Timeout is longer than OCR clients (30s) because TTS synthesis takes more time.
     *
     * @return configured RestTemplate instance
     */
    @Bean
    public RestTemplate minimaxRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(60000); // 60 seconds
        factory.setReadTimeout(60000);    // 60 seconds

        RestTemplate restTemplate = new RestTemplate(factory);
        logger.info("Minimax RestTemplate configured with 60-second timeout");
        return restTemplate;
    }
}
