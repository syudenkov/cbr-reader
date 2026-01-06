package com.cbrviewer.config;

import com.cbrviewer.repository.LlmConfigRepository;
import com.cbrviewer.util.EncryptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class for OpenAI client dependencies.
 * Provides beans for RestTemplate with timeout configuration and EncryptionUtil.
 */
@Configuration
public class OpenAiConfig {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiConfig.class);

    @Value("${app.security.encryption-key}")
    private String masterKey;

    private final LlmConfigRepository llmConfigRepository;

    public OpenAiConfig(LlmConfigRepository llmConfigRepository) {
        this.llmConfigRepository = llmConfigRepository;
        logger.info("OpenAiConfig initialized");
    }

    /**
     * Creates a RestTemplate configured with 30-second timeout for OpenAI API calls.
     *
     * @return configured RestTemplate instance
     */
    @Bean
    public RestTemplate openAiRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000); // 30 seconds in milliseconds
        factory.setReadTimeout(30000);    // 30 seconds in milliseconds

        RestTemplate restTemplate = new RestTemplate(factory);
        logger.info("OpenAI RestTemplate configured with 30-second timeout");
        return restTemplate;
    }

    /**
     * Creates an EncryptionUtil instance for encrypting/decrypting API keys.
     *
     * @return EncryptionUtil initialized with master key
     * @throws IllegalArgumentException if master key is invalid
     */
    @Bean
    public EncryptionUtil encryptionUtil() {
        logger.info("Initializing EncryptionUtil with master key from configuration");
        return new EncryptionUtil(masterKey);
    }
}
