package com.cbrviewer.service;

import com.cbrviewer.dto.LlmConfigDto;
import com.cbrviewer.dto.LlmConfigTestResult;
import com.cbrviewer.dto.UpdateLlmConfigRequest;
import com.cbrviewer.exception.InvalidApiKeyException;
import com.cbrviewer.exception.LlmConfigNotFoundException;
import com.cbrviewer.exception.LlmConfigTestException;
import com.cbrviewer.model.LlmConfig;
import com.cbrviewer.repository.LlmConfigRepository;
import com.cbrviewer.util.EncryptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LlmConfigService {

    private static final Logger logger = LoggerFactory.getLogger(LlmConfigService.class);

    private final LlmConfigRepository llmConfigRepository;
    private final EncryptionUtil encryptionUtil;
    private final AuditLogService auditLogService;
    private final RestTemplate restTemplate;

    public LlmConfigService(
        LlmConfigRepository llmConfigRepository,
        EncryptionUtil encryptionUtil,
        AuditLogService auditLogService,
        RestTemplate restTemplate
    ) {
        this.llmConfigRepository = llmConfigRepository;
        this.encryptionUtil = encryptionUtil;
        this.auditLogService = auditLogService;
        this.restTemplate = restTemplate;
    }

    /**
     * Get all LLM configurations with masked API keys.
     *
     * @return list of LlmConfigDto with masked API keys
     */
    public List<LlmConfigDto> getAllConfigs() {
        List<LlmConfig> configs = llmConfigRepository.findAll();
        return configs.stream()
            .map(config -> {
                String maskedKey = maskDecryptedKey(config);
                return LlmConfigDto.fromLlmConfig(config, maskedKey);
            })
            .collect(Collectors.toList());
    }

    /**
     * Get a single LLM configuration by ID with masked API key.
     *
     * @param id config ID
     * @return LlmConfigDto with masked API key
     * @throws LlmConfigNotFoundException if config not found
     */
    public LlmConfigDto getConfigById(Long id) {
        LlmConfig config = llmConfigRepository.findById(id)
            .orElseThrow(() -> new LlmConfigNotFoundException("LLM config not found: " + id));

        String maskedKey = maskDecryptedKey(config);
        return LlmConfigDto.fromLlmConfig(config, maskedKey);
    }

    /**
     * Update LLM configuration with optional field updates.
     *
     * @param id config ID
     * @param request update request with optional fields
     * @param currentAdminId admin user ID for audit logging
     * @return updated LlmConfigDto with masked API key
     * @throws LlmConfigNotFoundException if config not found
     * @throws InvalidApiKeyException if API key format is invalid
     */
    @Transactional
    public LlmConfigDto updateConfig(Long id, UpdateLlmConfigRequest request, Long currentAdminId) {
        LlmConfig config = llmConfigRepository.findById(id)
            .orElseThrow(() -> new LlmConfigNotFoundException("LLM config not found: " + id));

        // Update API key if provided
        if (request.getApiKey() != null && !request.getApiKey().trim().isEmpty()) {
            String apiKey = request.getApiKey().trim();

            // Validate API key format
            validateApiKeyFormat(config.getProvider(), apiKey);

            // Encrypt and save
            try {
                String encrypted = encryptionUtil.encrypt(apiKey);
                config.setApiKeyEncrypted(encrypted);
                logger.info("API key updated for provider: {}", config.getProvider());
            } catch (EncryptionUtil.EncryptionException e) {
                logger.error("Failed to encrypt API key for provider {}: {}", config.getProvider(), e.getMessage());
                throw new RuntimeException("Failed to encrypt API key", e);
            }
        }

        // Update model name if provided
        if (request.getModelName() != null) {
            config.setModelName(request.getModelName());
        }

        // Update isActive if provided
        if (request.getIsActive() != null) {
            config.setIsActive(request.getIsActive());
        }

        // Update priority if provided
        if (request.getPriority() != null) {
            config.setPriority(request.getPriority());
        }

        LlmConfig saved = llmConfigRepository.save(config);

        // Create audit log
        auditLogService.log(
            currentAdminId,
            "LLM_CONFIG_UPDATE",
            "LlmConfig",
            saved.getId(),
            String.format("{\"provider\": \"%s\"}", saved.getProvider())
        );

        String maskedKey = maskDecryptedKey(saved);
        return LlmConfigDto.fromLlmConfig(saved, maskedKey);
    }

    /**
     * Test LLM provider connection by making a lightweight API call.
     *
     * @param id config ID
     * @param currentAdminId admin user ID for audit logging
     * @return test result with success status, message, and latency
     * @throws LlmConfigNotFoundException if config not found
     */
    public LlmConfigTestResult testConfig(Long id, Long currentAdminId) {
        LlmConfig config = llmConfigRepository.findById(id)
            .orElseThrow(() -> new LlmConfigNotFoundException("LLM config not found: " + id));

        long startTime = System.currentTimeMillis();

        try {
            switch (config.getProvider().toLowerCase()) {
                case "openai":
                    testOpenAiConnection(config);
                    break;
                case "claude":
                    testClaudeConnection(config);
                    break;
                case "minimax":
                    testMinimaxConnection(config);
                    break;
                default:
                    throw new LlmConfigTestException("Unsupported provider: " + config.getProvider());
            }

            long latency = System.currentTimeMillis() - startTime;

            // Log successful test
            auditLogService.log(
                currentAdminId,
                "LLM_CONFIG_TEST",
                "LlmConfig",
                id,
                String.format("{\"provider\": \"%s\", \"success\": true}", config.getProvider())
            );

            return new LlmConfigTestResult(
                config.getProvider(),
                true,
                "Connection successful",
                latency,
                LocalDateTime.now()
            );

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;

            logger.error("LLM config test failed for {}: {}", config.getProvider(), e.getMessage());

            // Log failed test
            auditLogService.log(
                currentAdminId,
                "LLM_CONFIG_TEST",
                "LlmConfig",
                id,
                String.format("{\"provider\": \"%s\", \"success\": false, \"error\": \"%s\"}",
                    config.getProvider(), e.getMessage())
            );

            return new LlmConfigTestResult(
                config.getProvider(),
                false,
                "Connection failed: " + e.getMessage(),
                latency,
                LocalDateTime.now()
            );
        }
    }

    /**
     * Decrypt API key and mask it for display (never return plaintext).
     *
     * @param config LlmConfig entity
     * @return masked API key (e.g., "sk-proj-...1234")
     */
    private String maskDecryptedKey(LlmConfig config) {
        try {
            String plaintext = encryptionUtil.decrypt(config.getApiKeyEncrypted());
            return maskApiKey(plaintext);
        } catch (EncryptionUtil.EncryptionException e) {
            logger.error("Failed to decrypt API key for provider {}: {}", config.getProvider(), e.getMessage());
            return "***";  // Return safe placeholder for corrupted data
        }
    }

    /**
     * Mask API key using prefix + last 4 characters.
     *
     * @param plaintextKey plaintext API key
     * @return masked key (e.g., "sk-proj-...1234")
     */
    private String maskApiKey(String plaintextKey) {
        if (plaintextKey == null || plaintextKey.length() < 8) {
            return "***";  // Too short to mask safely
        }

        // Find prefix (up to first dash or 8 chars, whichever is shorter)
        int dashIndex = plaintextKey.indexOf('-');
        String prefix = dashIndex > 0 && dashIndex < 8
            ? plaintextKey.substring(0, dashIndex)
            : plaintextKey.substring(0, Math.min(8, plaintextKey.length()));

        // Get last 4 characters
        String last4 = plaintextKey.substring(plaintextKey.length() - 4);

        return prefix + "-..." + last4;
    }

    /**
     * Validate API key format for specific provider.
     *
     * @param provider provider name (openai, claude, minimax)
     * @param apiKey plaintext API key
     * @throws InvalidApiKeyException if format is invalid
     */
    private void validateApiKeyFormat(String provider, String apiKey) {
        switch (provider.toLowerCase()) {
            case "openai":
                if (!apiKey.startsWith("sk-proj-") && !apiKey.startsWith("sk-")) {
                    throw new InvalidApiKeyException("OpenAI API key must start with 'sk-' or 'sk-proj-'");
                }
                break;
            case "claude":
                if (!apiKey.startsWith("sk-ant-")) {
                    throw new InvalidApiKeyException("Claude API key must start with 'sk-ant-'");
                }
                break;
            case "minimax":
                if (apiKey.length() < 16) {
                    throw new InvalidApiKeyException("Minimax API key too short (minimum 16 characters)");
                }
                break;
            default:
                logger.warn("Unknown provider, skipping API key format validation: {}", provider);
        }
    }

    /**
     * Test OpenAI connection by calling models endpoint.
     *
     * @param config LlmConfig for OpenAI
     * @throws Exception if test fails
     */
    private void testOpenAiConnection(LlmConfig config) throws Exception {
        String apiKey = encryptionUtil.decrypt(config.getApiKeyEncrypted());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                "https://api.openai.com/v1/models",
                HttpMethod.GET,
                request,
                String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new LlmConfigTestException("OpenAI API test failed: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                throw new LlmConfigTestException("Invalid OpenAI API key (authentication failed)");
            }
            throw new LlmConfigTestException("OpenAI API test failed: " + e.getMessage());
        }
    }

    /**
     * Test Claude connection by calling messages endpoint with minimal payload.
     *
     * @param config LlmConfig for Claude
     * @throws Exception if test fails
     */
    private void testClaudeConnection(LlmConfig config) throws Exception {
        String apiKey = encryptionUtil.decrypt(config.getApiKeyEncrypted());

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");
        headers.set("Content-Type", "application/json");

        // Minimal test payload (low cost)
        String testPayload = "{\"model\":\"claude-3-haiku-20240307\",\"max_tokens\":1,\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}";

        HttpEntity<String> request = new HttpEntity<>(testPayload, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                "https://api.anthropic.com/v1/messages",
                HttpMethod.POST,
                request,
                String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new LlmConfigTestException("Claude API test failed: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                throw new LlmConfigTestException("Invalid Claude API key (authentication failed)");
            }
            throw new LlmConfigTestException("Claude API test failed: " + e.getMessage());
        }
    }

    /**
     * Test Minimax connection by calling text-to-speech endpoint.
     *
     * @param config LlmConfig for Minimax
     * @throws Exception if test fails
     */
    private void testMinimaxConnection(LlmConfig config) throws Exception {
        String apiKey = encryptionUtil.decrypt(config.getApiKeyEncrypted());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Content-Type", "application/json");

        // Minimal TTS test payload
        String testPayload = "{\"text\":\"test\",\"model\":\"speech-01\",\"voice_id\":\"male-qn-qingse\"}";

        HttpEntity<String> request = new HttpEntity<>(testPayload, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                "https://api.minimax.chat/v1/text_to_speech",
                HttpMethod.POST,
                request,
                String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new LlmConfigTestException("Minimax API test failed: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                throw new LlmConfigTestException("Invalid Minimax API key (authentication failed)");
            }
            throw new LlmConfigTestException("Minimax API test failed: " + e.getMessage());
        }
    }
}
