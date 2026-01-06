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
 * Client for Claude Vision API (Claude 3.5 Sonnet) OCR extraction.
 * Implements retry logic with exponential backoff and comprehensive error handling.
 * Used as fallback when OpenAI provider fails.
 */
@Service
public class ClaudeClient {

    private static final Logger logger = LoggerFactory.getLogger(ClaudeClient.class);

    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String PROVIDER_NAME = "claude";
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

    public ClaudeClient(
        RestTemplate claudeRestTemplate,
        EncryptionUtil encryptionUtil,
        LlmConfigRepository llmConfigRepository,
        SystemLogRepository systemLogRepository
    ) {
        this.restTemplate = claudeRestTemplate;
        this.encryptionUtil = encryptionUtil;
        this.llmConfigRepository = llmConfigRepository;
        this.systemLogRepository = systemLogRepository;
        this.objectMapper = new ObjectMapper();
        logger.info("ClaudeClient initialized");
    }

    /**
     * Extracts OCR data from image bytes using Claude 3.5 Sonnet Vision API.
     *
     * @param imageBytes raw image data (JPEG/PNG)
     * @return OcrResponse containing dialogue segments and metadata
     * @throws OcrException if all retry attempts fail or timeout occurs
     */
    public OcrResponse ocrExtract(byte[] imageBytes) throws OcrException {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Image bytes cannot be null or empty");
        }

        // Fetch Claude configuration
        LlmConfig config = llmConfigRepository.findByProvider(PROVIDER_NAME)
            .orElseThrow(() -> new OcrException("Claude configuration not found in llm_config table"));

        if (config.getIsActive() == null || config.getIsActive() != 1) {
            throw new OcrException("Claude provider is not active (isActive != 1)");
        }

        // Decrypt API key
        String apiKey;
        try {
            apiKey = encryptionUtil.decrypt(config.getApiKeyEncrypted());
            logger.info("API key loaded from LlmConfig (provider: claude)");
        } catch (EncryptionUtil.EncryptionException e) {
            logger.error("Failed to decrypt Claude API key: {}", e.getMessage());
            logToDatabase("ERROR", "Failed to decrypt Claude API key: " + e.getMessage(), e);
            throw new OcrException("Failed to decrypt API key", e);
        }

        // Retry logic with exponential backoff
        long startTime = System.currentTimeMillis();
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                logger.info("Claude OCR attempt {}/{}", attempt, MAX_RETRIES);

                OcrResponse response = makeApiCall(imageBytes, apiKey, config.getModelName());

                long latency = System.currentTimeMillis() - startTime;
                logger.info("Claude OCR succeeded: {} segments, {} tokens, {} ms",
                          response.getSegments().size(), response.getTokensUsed(), latency);

                logToDatabase("INFO",
                    String.format("Claude OCR success: %d tokens, %d ms, %d segments",
                                response.getTokensUsed(), latency, response.getSegments().size()),
                    null);

                return response;

            } catch (HttpClientErrorException.TooManyRequests e) {
                // HTTP 429 - Rate limit
                lastException = e;
                logger.warn("Claude API attempt {}/{} failed: Rate limit (429)", attempt, MAX_RETRIES);

                if (attempt < MAX_RETRIES) {
                    long waitTime = (long) Math.pow(2, attempt - 1) * 1000; // 1s, 2s, 4s
                    logger.info("Waiting {} ms before retry", waitTime);
                    logToDatabase("WARN",
                        String.format("Claude API attempt %d/%d: Rate limit, retrying after %d ms",
                                    attempt, MAX_RETRIES, waitTime),
                        null);
                    sleep(waitTime);
                }

            } catch (HttpServerErrorException e) {
                // HTTP 500, 503 - Server errors
                lastException = e;
                logger.warn("Claude API attempt {}/{} failed: Server error ({})",
                          attempt, MAX_RETRIES, e.getStatusCode());

                if (attempt < MAX_RETRIES) {
                    long waitTime = (long) Math.pow(2, attempt - 1) * 1000;
                    logger.info("Waiting {} ms before retry", waitTime);
                    logToDatabase("WARN",
                        String.format("Claude API attempt %d/%d: Server error %s, retrying after %d ms",
                                    attempt, MAX_RETRIES, e.getStatusCode(), waitTime),
                        null);
                    sleep(waitTime);
                }

            } catch (ResourceAccessException e) {
                // Timeout or connection error
                lastException = e;
                logger.warn("Claude API attempt {}/{} failed: Timeout or connection error",
                          attempt, MAX_RETRIES);

                if (attempt < MAX_RETRIES) {
                    long waitTime = (long) Math.pow(2, attempt - 1) * 1000;
                    logger.info("Waiting {} ms before retry", waitTime);
                    logToDatabase("WARN",
                        String.format("Claude API attempt %d/%d: Timeout, retrying after %d ms",
                                    attempt, MAX_RETRIES, waitTime),
                        null);
                    sleep(waitTime);
                }

            } catch (HttpClientErrorException e) {
                // HTTP 400, 401, 403 - Don't retry client errors
                logger.error("Claude API failed with client error ({}): {}",
                           e.getStatusCode(), e.getMessage());
                logToDatabase("ERROR",
                    String.format("Claude API client error %s: %s", e.getStatusCode(), e.getMessage()),
                    e);
                throw new OcrException("Claude API client error: " + e.getStatusCode(), e);

            } catch (Exception e) {
                // Other unexpected errors
                logger.error("Claude API attempt {}/{} failed with unexpected error: {}",
                           attempt, MAX_RETRIES, e.getMessage());
                logToDatabase("ERROR",
                    String.format("Claude API attempt %d/%d unexpected error: %s",
                                attempt, MAX_RETRIES, e.getMessage()),
                    e);
                throw new OcrException("Claude API unexpected error", e);
            }
        }

        // All retries exhausted
        long totalLatency = System.currentTimeMillis() - startTime;
        String errorMessage = String.format(
            "Claude OCR failed after %d attempts (%d ms total): %s",
            MAX_RETRIES, totalLatency,
            lastException != null ? lastException.getMessage() : "Unknown error"
        );

        logger.error(errorMessage);
        logToDatabase("ERROR", errorMessage, lastException);
        throw new OcrException(errorMessage, lastException);
    }

    /**
     * Makes a single API call to Claude Vision API.
     */
    private OcrResponse makeApiCall(byte[] imageBytes, String apiKey, String modelName) throws Exception {
        // Convert image to Base64 (no data URI prefix for Claude!)
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        // Build image source object
        Map<String, Object> imageSource = new HashMap<>();
        imageSource.put("type", "base64");
        imageSource.put("media_type", "image/jpeg");
        imageSource.put("data", base64Image);

        // Build image object
        Map<String, Object> imageObject = new HashMap<>();
        imageObject.put("type", "image");
        imageObject.put("source", imageSource);

        // Build content array
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of("type", "text", "text", OCR_PROMPT));
        content.add(imageObject);

        // Build message
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", content);

        // Build request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName != null ? modelName : "claude-3-5-sonnet-20241022");
        requestBody.put("max_tokens", MAX_TOKENS);
        requestBody.put("messages", List.of(message));

        // Build headers (Claude-specific)
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", apiKey); // NOT "Authorization: Bearer"
        headers.set("anthropic-version", "2023-06-01"); // Required API version header
        headers.set("Content-Type", "application/json");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        // Make API call
        ResponseEntity<String> response = restTemplate.exchange(
            CLAUDE_API_URL,
            HttpMethod.POST,
            request,
            String.class
        );

        // Parse response
        return parseResponse(response.getBody());
    }

    /**
     * Parses Claude API response and extracts dialogue segments.
     */
    private OcrResponse parseResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        // Check for error response
        if (root.has("type") && "error".equals(root.get("type").asText())) {
            String errorMsg = root.path("error").path("message").asText("Unknown error");
            throw new OcrException("Claude API error: " + errorMsg);
        }

        // Extract content from content[0].text
        JsonNode contentNode = root.path("content");
        if (contentNode.isEmpty() || !contentNode.isArray() || contentNode.size() == 0) {
            throw new OcrException("Invalid API response: missing 'content' array");
        }

        String contentJson = contentNode.get(0).path("text").asText();
        if (contentJson == null || contentJson.isEmpty()) {
            throw new OcrException("Invalid API response: empty content text");
        }

        // Parse content as JSON array
        OcrResponse.DialogueSegment[] segments;
        try {
            segments = objectMapper.readValue(contentJson, OcrResponse.DialogueSegment[].class);
        } catch (Exception e) {
            logger.error("Failed to parse dialogue segments from content: {}", contentJson);
            throw new OcrException("Failed to parse dialogue segments: " + e.getMessage(), e);
        }

        // Calculate total tokens (Claude splits into input + output)
        int inputTokens = root.path("usage").path("input_tokens").asInt(0);
        int outputTokens = root.path("usage").path("output_tokens").asInt(0);
        int tokensUsed = inputTokens + outputTokens;

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
