package com.cbrviewer.client;

import com.cbrviewer.dto.OcrResponse;
import com.cbrviewer.exception.OcrException;
import com.cbrviewer.model.LlmConfig;
import com.cbrviewer.model.SystemLog;
import com.cbrviewer.repository.LlmConfigRepository;
import com.cbrviewer.repository.SystemLogRepository;
import com.cbrviewer.util.EncryptionUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

/**
 * Client for OpenAI Vision API (GPT-4o) OCR extraction.
 * Implements retry logic with exponential backoff and comprehensive error handling.
 */
@Service
public class OpenAiClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiClient.class);

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String PROVIDER_NAME = "openai";
    private static final int MAX_RETRIES = 3;
    private static final int MAX_TOKENS = 1000;

    private static final String OCR_PROMPT =
        "Extract all dialogue from this comic panel. For each speech bubble, " +
        "identify the speaker character and their exact words. " +
        "Return ONLY a JSON array with this structure: " +
        "[{\"speaker\": \"Character Name\", \"text\": \"Dialogue text\"}]. " +
        "If you cannot identify the speaker, use \"Unknown\". " +
        "If there is no dialogue, return an empty array: []";

    private final RestTemplate restTemplate;
    private final EncryptionUtil encryptionUtil;
    private final LlmConfigRepository llmConfigRepository;
    private final SystemLogRepository systemLogRepository;
    private final ObjectMapper objectMapper;

    public OpenAiClient(
        RestTemplate openAiRestTemplate,
        EncryptionUtil encryptionUtil,
        LlmConfigRepository llmConfigRepository,
        SystemLogRepository systemLogRepository
    ) {
        this.restTemplate = openAiRestTemplate;
        this.encryptionUtil = encryptionUtil;
        this.llmConfigRepository = llmConfigRepository;
        this.systemLogRepository = systemLogRepository;
        this.objectMapper = new ObjectMapper();
        logger.info("OpenAiClient initialized");
    }

    /**
     * Extracts OCR data from image bytes using OpenAI GPT-4o Vision API.
     *
     * @param imageBytes raw image data (JPEG/PNG)
     * @return OcrResponse containing dialogue segments and metadata
     * @throws OcrException if all retry attempts fail or timeout occurs
     */
    public OcrResponse ocrExtract(byte[] imageBytes) throws OcrException {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Image bytes cannot be null or empty");
        }

        // Fetch OpenAI configuration
        LlmConfig config = llmConfigRepository.findByProvider(PROVIDER_NAME)
            .orElseThrow(() -> new OcrException("OpenAI configuration not found in llm_config table"));

        if (config.getIsActive() == null || config.getIsActive() != 1) {
            throw new OcrException("OpenAI provider is not active (isActive != 1)");
        }

        // Decrypt API key
        String apiKey;
        try {
            apiKey = encryptionUtil.decrypt(config.getApiKeyEncrypted());
            logger.info("API key loaded from LlmConfig (provider: openai)");
        } catch (EncryptionUtil.EncryptionException e) {
            logger.error("Failed to decrypt OpenAI API key: {}", e.getMessage());
            logToDatabase("ERROR", "Failed to decrypt OpenAI API key: " + e.getMessage(), e);
            throw new OcrException("Failed to decrypt API key", e);
        }

        // Retry logic with exponential backoff
        long startTime = System.currentTimeMillis();
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                logger.info("OpenAI OCR attempt {}/{}", attempt, MAX_RETRIES);

                OcrResponse response = makeApiCall(imageBytes, apiKey, config.getModelName());

                long latency = System.currentTimeMillis() - startTime;
                logger.info("OpenAI OCR succeeded: {} segments, {} tokens, {} ms",
                          response.getSegments().size(), response.getTokensUsed(), latency);

                logToDatabase("INFO",
                    String.format("OpenAI OCR success: %d tokens, %d ms, %d segments",
                                response.getTokensUsed(), latency, response.getSegments().size()),
                    null);

                return response;

            } catch (HttpClientErrorException.TooManyRequests e) {
                // HTTP 429 - Rate limit
                lastException = e;
                logger.warn("OpenAI API attempt {}/{} failed: Rate limit (429)", attempt, MAX_RETRIES);

                if (attempt < MAX_RETRIES) {
                    long waitTime = (long) Math.pow(2, attempt - 1) * 1000; // 1s, 2s, 4s
                    logger.info("Waiting {} ms before retry", waitTime);
                    logToDatabase("WARN",
                        String.format("OpenAI API attempt %d/%d: Rate limit, retrying after %d ms",
                                    attempt, MAX_RETRIES, waitTime),
                        null);
                    sleep(waitTime);
                }

            } catch (HttpServerErrorException e) {
                // HTTP 500, 503 - Server errors
                lastException = e;
                logger.warn("OpenAI API attempt {}/{} failed: Server error ({})",
                          attempt, MAX_RETRIES, e.getStatusCode());

                if (attempt < MAX_RETRIES) {
                    long waitTime = (long) Math.pow(2, attempt - 1) * 1000;
                    logger.info("Waiting {} ms before retry", waitTime);
                    logToDatabase("WARN",
                        String.format("OpenAI API attempt %d/%d: Server error %s, retrying after %d ms",
                                    attempt, MAX_RETRIES, e.getStatusCode(), waitTime),
                        null);
                    sleep(waitTime);
                }

            } catch (ResourceAccessException e) {
                // Timeout or connection error
                lastException = e;
                logger.warn("OpenAI API attempt {}/{} failed: Timeout or connection error",
                          attempt, MAX_RETRIES);

                if (attempt < MAX_RETRIES) {
                    long waitTime = (long) Math.pow(2, attempt - 1) * 1000;
                    logger.info("Waiting {} ms before retry", waitTime);
                    logToDatabase("WARN",
                        String.format("OpenAI API attempt %d/%d: Timeout, retrying after %d ms",
                                    attempt, MAX_RETRIES, waitTime),
                        null);
                    sleep(waitTime);
                }

            } catch (HttpClientErrorException e) {
                // HTTP 400, 401, 403 - Don't retry client errors
                logger.error("OpenAI API failed with client error ({}): {}",
                           e.getStatusCode(), e.getMessage());
                logToDatabase("ERROR",
                    String.format("OpenAI API client error %s: %s", e.getStatusCode(), e.getMessage()),
                    e);
                throw new OcrException("OpenAI API client error: " + e.getStatusCode(), e);

            } catch (Exception e) {
                // Other unexpected errors
                logger.error("OpenAI API attempt {}/{} failed with unexpected error: {}",
                           attempt, MAX_RETRIES, e.getMessage());
                logToDatabase("ERROR",
                    String.format("OpenAI API attempt %d/%d unexpected error: %s",
                                attempt, MAX_RETRIES, e.getMessage()),
                    e);
                throw new OcrException("OpenAI API unexpected error", e);
            }
        }

        // All retries exhausted
        long totalLatency = System.currentTimeMillis() - startTime;
        String errorMessage = String.format(
            "OpenAI OCR failed after %d attempts (%d ms total): %s",
            MAX_RETRIES, totalLatency,
            lastException != null ? lastException.getMessage() : "Unknown error"
        );

        logger.error(errorMessage);
        logToDatabase("ERROR", errorMessage, lastException);
        throw new OcrException(errorMessage, lastException);
    }

    /**
     * Makes a single API call to OpenAI Vision API.
     */
    private OcrResponse makeApiCall(byte[] imageBytes, String apiKey, String modelName) throws Exception {
        // Convert image to Base64 data URI
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String dataUri = "data:image/jpeg;base64," + base64Image;

        // Build request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName != null ? modelName : "gpt-4o");
        requestBody.put("max_tokens", MAX_TOKENS);

        // Build message content
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of("type", "text", "text", OCR_PROMPT));
        content.add(Map.of("type", "image_url",
                          "image_url", Map.of("url", dataUri)));

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", content);

        requestBody.put("messages", List.of(message));

        // Build headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Content-Type", "application/json");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        // Make API call
        ResponseEntity<String> response = restTemplate.exchange(
            OPENAI_API_URL,
            HttpMethod.POST,
            request,
            String.class
        );

        // Parse response
        return parseResponse(response.getBody());
    }

    /**
     * Parses OpenAI API response and extracts dialogue segments.
     */
    private OcrResponse parseResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        // Extract content from choices[0].message.content
        JsonNode choicesNode = root.path("choices");
        if (choicesNode.isEmpty() || !choicesNode.isArray() || choicesNode.size() == 0) {
            throw new OcrException("Invalid API response: missing 'choices' array");
        }

        String contentJson = choicesNode.get(0).path("message").path("content").asText();
        if (contentJson == null || contentJson.isEmpty()) {
            throw new OcrException("Invalid API response: empty content");
        }

        // Parse content as JSON array
        OcrResponse.DialogueSegment[] segments;
        try {
            segments = objectMapper.readValue(contentJson, OcrResponse.DialogueSegment[].class);
        } catch (Exception e) {
            logger.error("Failed to parse dialogue segments from content: {}", contentJson);
            throw new OcrException("Failed to parse dialogue segments: " + e.getMessage(), e);
        }

        // Extract token usage
        int tokensUsed = root.path("usage").path("total_tokens").asInt(0);

        // Build response
        OcrResponse ocrResponse = new OcrResponse();
        ocrResponse.setSegments(Arrays.asList(segments));
        ocrResponse.setProvider(PROVIDER_NAME);
        ocrResponse.setTokensUsed(tokensUsed);

        return ocrResponse;
    }

    /**
     * Logs message to system_logs table.
     */
    private void logToDatabase(String level, String message, Exception exception) {
        try {
            SystemLog log = new SystemLog(level, message);
            if (exception != null) {
                log.setStackTrace(getStackTrace(exception));
            }
            systemLogRepository.save(log);
        } catch (Exception e) {
            // Log to console if database logging fails
            logger.error("Failed to save log to database: {}", e.getMessage());
        }
    }

    /**
     * Converts exception stack trace to string.
     */
    private String getStackTrace(Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Thread sleep helper.
     */
    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Sleep interrupted: {}", e.getMessage());
        }
    }
}
