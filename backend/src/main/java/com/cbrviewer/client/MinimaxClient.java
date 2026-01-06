package com.cbrviewer.client;

import com.cbrviewer.exception.MinimaxException;
import com.cbrviewer.model.LlmConfig;
import com.cbrviewer.model.SystemLog;
import com.cbrviewer.repository.LlmConfigRepository;
import com.cbrviewer.repository.SystemLogRepository;
import com.cbrviewer.util.EncryptionUtil;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Client for Minimax TTS API.
 * Implements text-to-speech synthesis with retry logic, error handling, and audio validation.
 *
 * Features:
 * - Retry logic with exponential backoff (3 attempts: 1s, 2s, 4s delays)
 * - Text chunking for long dialogue (MVP stub - truncates at 500 chars)
 * - MP3 format validation (magic byte checking)
 * - Comprehensive logging and database persistence
 * - API key encryption/decryption
 */
@Service
public class MinimaxClient {

    private static final Logger logger = LoggerFactory.getLogger(MinimaxClient.class);

    private static final String MINIMAX_API_URL = "https://api.minimax.chat/v1/tts";
    private static final String PROVIDER_NAME = "minimax";
    private static final int MAX_RETRIES = 3;
    private static final int MAX_TEXT_LENGTH = 500; // Character limit for single synthesis

    private final RestTemplate restTemplate;
    private final EncryptionUtil encryptionUtil;
    private final LlmConfigRepository llmConfigRepository;
    private final SystemLogRepository systemLogRepository;

    /**
     * Constructor for MinimaxClient.
     * Dependencies are injected via Spring's constructor injection.
     *
     * @param minimaxRestTemplate RestTemplate configured with 60-second timeout
     * @param encryptionUtil utility for API key decryption
     * @param llmConfigRepository repository for accessing llm_config table
     * @param systemLogRepository repository for logging to system_logs table
     */
    public MinimaxClient(
        RestTemplate minimaxRestTemplate,
        EncryptionUtil encryptionUtil,
        LlmConfigRepository llmConfigRepository,
        SystemLogRepository systemLogRepository
    ) {
        this.restTemplate = minimaxRestTemplate;
        this.encryptionUtil = encryptionUtil;
        this.llmConfigRepository = llmConfigRepository;
        this.systemLogRepository = systemLogRepository;
        logger.info("MinimaxClient initialized");
    }

    /**
     * Synthesizes speech from text using Minimax TTS API.
     * Handles long text by chunking (MVP stub - truncates to 500 chars).
     *
     * @param text the dialogue text to synthesize (must not be null or empty)
     * @param voiceId the Minimax voice ID (e.g., "hero_deep_male", "narrator_neutral")
     * @return MP3 audio bytes
     * @throws MinimaxException if synthesis fails after all retries or configuration error
     */
    public byte[] synthesize(String text, String voiceId) throws MinimaxException {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }

        if (voiceId == null || voiceId.isEmpty()) {
            throw new IllegalArgumentException("Voice ID cannot be null or empty");
        }

        // If text is short, synthesize directly
        if (text.length() <= MAX_TEXT_LENGTH) {
            return synthesizeSingle(text, voiceId);
        }

        // If text is long, chunk and synthesize (MVP stub)
        logger.warn("Text length {} exceeds limit {}, truncating for MVP",
                  text.length(), MAX_TEXT_LENGTH);
        return synthesizeChunked(text, voiceId);
    }

    /**
     * Synthesizes a single text segment (within character limit).
     * Implements retry logic with exponential backoff.
     *
     * @param text text to synthesize (≤ 500 chars)
     * @param voiceId Minimax voice ID
     * @return MP3 audio bytes
     * @throws MinimaxException if synthesis fails after all retries
     */
    private byte[] synthesizeSingle(String text, String voiceId) throws MinimaxException {
        // Fetch Minimax configuration
        LlmConfig config = llmConfigRepository.findByProvider(PROVIDER_NAME)
            .orElseThrow(() -> new MinimaxException("Minimax configuration not found in llm_config table"));

        if (config.getIsActive() == null || config.getIsActive() != 1) {
            throw new MinimaxException("Minimax provider is not active (isActive != 1)");
        }

        // Decrypt API key
        String apiKey;
        try {
            apiKey = encryptionUtil.decrypt(config.getApiKeyEncrypted());
            logger.debug("API key loaded from LlmConfig (provider: minimax)");
        } catch (EncryptionUtil.EncryptionException e) {
            logger.error("Failed to decrypt Minimax API key: {}", e.getMessage());
            logToDatabase("ERROR", "Failed to decrypt Minimax API key: " + e.getMessage(), e);
            throw new MinimaxException("Failed to decrypt API key", e);
        }

        // Retry logic with exponential backoff
        long startTime = System.currentTimeMillis();
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                logger.info("Minimax TTS attempt {}/{} for voice={}, textLen={}",
                          attempt, MAX_RETRIES, voiceId, text.length());

                byte[] audioBytes = makeApiCall(text, voiceId, apiKey);

                long latency = System.currentTimeMillis() - startTime;
                logger.info("Minimax TTS succeeded: {} bytes, {} ms, voice={}, textLen={}",
                          audioBytes.length, latency, voiceId, text.length());

                logToDatabase("INFO",
                    String.format("Minimax TTS success: %d bytes, %d ms, voice=%s, textLen=%d",
                                audioBytes.length, latency, voiceId, text.length()),
                    null);

                return audioBytes;

            } catch (HttpClientErrorException.TooManyRequests e) {
                // HTTP 429 - Rate limit
                lastException = e;
                logger.warn("Minimax API attempt {}/{} failed: Rate limit (429)",
                          attempt, MAX_RETRIES);

                if (attempt < MAX_RETRIES) {
                    long waitTime = (long) Math.pow(2, attempt - 1) * 1000; // 1s, 2s, 4s
                    logger.info("Rate limit hit, waiting {} ms before retry", waitTime);
                    logToDatabase("WARN",
                        String.format("Minimax TTS attempt %d/%d: Rate limit, retrying after %d ms",
                                    attempt, MAX_RETRIES, waitTime),
                        null);
                    sleep(waitTime);
                }

            } catch (HttpServerErrorException e) {
                // HTTP 500, 503 - Server errors
                lastException = e;
                logger.warn("Minimax API attempt {}/{} failed: Server error ({})",
                          attempt, MAX_RETRIES, e.getStatusCode());

                if (attempt < MAX_RETRIES) {
                    long waitTime = (long) Math.pow(2, attempt - 1) * 1000;
                    logger.info("Server error, waiting {} ms before retry", waitTime);
                    logToDatabase("WARN",
                        String.format("Minimax TTS attempt %d/%d: Server error %s, retrying after %d ms",
                                    attempt, MAX_RETRIES, e.getStatusCode(), waitTime),
                        null);
                    sleep(waitTime);
                }

            } catch (ResourceAccessException e) {
                // Timeout or connection error
                lastException = e;
                logger.warn("Minimax API attempt {}/{} failed: Timeout or connection error",
                          attempt, MAX_RETRIES);

                if (attempt < MAX_RETRIES) {
                    long waitTime = (long) Math.pow(2, attempt - 1) * 1000;
                    logger.info("Timeout error, waiting {} ms before retry", waitTime);
                    logToDatabase("WARN",
                        String.format("Minimax TTS attempt %d/%d: Timeout, retrying after %d ms",
                                    attempt, MAX_RETRIES, waitTime),
                        null);
                    sleep(waitTime);
                }

            } catch (HttpClientErrorException e) {
                // HTTP 400, 401, 403 - Don't retry client errors
                logger.error("Minimax API failed with client error ({}): {}",
                           e.getStatusCode(), e.getMessage());
                logToDatabase("ERROR",
                    String.format("Minimax API client error %s: %s",
                                e.getStatusCode(), e.getMessage()),
                    e);
                throw new MinimaxException("Minimax API client error: " + e.getStatusCode(), e);

            } catch (Exception e) {
                // Other unexpected errors
                logger.error("Minimax API attempt {}/{} failed with unexpected error: {}",
                           attempt, MAX_RETRIES, e.getMessage());
                logToDatabase("ERROR",
                    String.format("Minimax API attempt %d/%d unexpected error: %s",
                                attempt, MAX_RETRIES, e.getMessage()),
                    e);
                throw new MinimaxException("Minimax API unexpected error", e);
            }
        }

        // All retries exhausted
        long totalLatency = System.currentTimeMillis() - startTime;
        String errorMessage = String.format(
            "Minimax TTS failed after %d attempts (%d ms total): %s",
            MAX_RETRIES, totalLatency,
            lastException != null ? lastException.getMessage() : "Unknown error"
        );

        logger.error(errorMessage);
        logToDatabase("ERROR", errorMessage, lastException);
        throw new MinimaxException(errorMessage, lastException);
    }

    /**
     * Handles long text synthesis by chunking.
     * MVP STUB: Truncates to first 500 chars instead of true chunking.
     * Future enhancement: split at sentence boundaries and concatenate audio.
     *
     * @param text long text (> 500 chars)
     * @param voiceId Minimax voice ID
     * @return MP3 audio bytes for first chunk
     * @throws MinimaxException if synthesis fails
     */
    private byte[] synthesizeChunked(String text, String voiceId) throws MinimaxException {
        // TODO: Implement full chunking in future iteration
        // - Split text at sentence boundaries (. ! ?)
        // - Synthesize each chunk separately
        // - Concatenate MP3 bytes using audio processing library

        logger.warn("Text chunking not fully implemented, using first {} chars", MAX_TEXT_LENGTH);
        logToDatabase("WARN",
            String.format("Text length %d exceeds limit, truncating to %d chars",
                        text.length(), MAX_TEXT_LENGTH),
            null);

        String truncatedText = text.substring(0, MAX_TEXT_LENGTH);
        return synthesizeSingle(truncatedText, voiceId);
    }

    /**
     * Makes a single API call to Minimax TTS endpoint.
     * Validates response and checks MP3 format.
     *
     * @param text dialogue text
     * @param voiceId Minimax voice ID
     * @param apiKey decrypted API key
     * @return MP3 audio bytes
     * @throws Exception on API errors or invalid response
     */
    private byte[] makeApiCall(String text, String voiceId, String apiKey) throws Exception {
        // Build request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("text", text);
        requestBody.put("voice", voiceId);
        requestBody.put("format", "mp3");

        // Build headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Content-Type", "application/json");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        // Make API call
        ResponseEntity<byte[]> response = restTemplate.exchange(
            MINIMAX_API_URL,
            HttpMethod.POST,
            request,
            byte[].class
        );

        // Validate response
        byte[] audioBytes = response.getBody();
        if (audioBytes == null || audioBytes.length == 0) {
            throw new MinimaxException("Minimax API returned empty audio data");
        }

        // Validate MP3 format (advisory - log warning but don't fail)
        if (!isMp3Format(audioBytes)) {
            logger.warn("Audio bytes do not have MP3 magic header (may be valid MP3 without ID3 tag)");
        }

        logger.debug("Minimax TTS API call succeeded: {} bytes, voice={}", audioBytes.length, voiceId);
        return audioBytes;
    }

    /**
     * Validates MP3 format by checking magic bytes.
     * Advisory check only - doesn't throw exception, just logs warning.
     *
     * MP3 format identifiers:
     * - Frame sync: 0xFF 0xFB or 0xFF 0xF3 or 0xFF 0xF2 (MPEG frame header)
     * - ID3 tag: "ID3" at start of file
     *
     * @param data audio bytes
     * @return true if MP3 magic bytes detected, false otherwise
     */
    private boolean isMp3Format(byte[] data) {
        if (data == null || data.length < 3) {
            return false;
        }

        // Check for MP3 frame header (0xFF followed by 0xE0-0xFF)
        if (data[0] == (byte) 0xFF && (data[1] & 0xE0) == 0xE0) {
            return true;
        }

        // Check for ID3 tag header ("ID3")
        if (data[0] == 'I' && data[1] == 'D' && data[2] == '3') {
            return true;
        }

        return false;
    }

    /**
     * Logs message to system_logs table.
     * Catches and suppresses database errors to prevent logging failures
     * from breaking TTS pipeline.
     *
     * @param level log level (INFO, WARN, ERROR)
     * @param message log message
     * @param exception optional exception (can be null)
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
     * Converts exception stack trace to string for database storage.
     *
     * @param exception the exception to convert
     * @return stack trace as string
     */
    private String getStackTrace(Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Thread sleep helper for retry backoff.
     * Handles InterruptedException and restores interrupt status.
     *
     * @param milliseconds duration to sleep
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
