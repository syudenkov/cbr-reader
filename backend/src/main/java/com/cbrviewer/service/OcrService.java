package com.cbrviewer.service;

import com.cbrviewer.client.ClaudeClient;
import com.cbrviewer.client.OpenAiClient;
import com.cbrviewer.dto.OcrResponse;
import com.cbrviewer.dto.PageOcrResult;
import com.cbrviewer.dto.PageOcrResult.SpeakerSegment;
import com.cbrviewer.dto.PageOcrResult.VoiceCharacteristics;
import com.cbrviewer.exception.OcrException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * OCR orchestration service with OpenAI → Claude fallback cascade.
 *
 * This service implements resilient page-level LLM fallback for comic OCR extraction:
 * 1. Try OpenAI Vision API (3 retries with exponential backoff)
 * 2. On failure, try Claude Vision API (3 retries)
 * 3. On both failures, return failed result (job continues to next page)
 *
 * The service also performs voice characteristics inference using simple heuristics
 * to map speakers to appropriate TTS voice profiles.
 */
@Service
public class OcrService {

    private static final Logger logger = LoggerFactory.getLogger(OcrService.class);

    private final OpenAiClient openAiClient;
    private final ClaudeClient claudeClient;

    public OcrService(OpenAiClient openAiClient, ClaudeClient claudeClient) {
        this.openAiClient = openAiClient;
        this.claudeClient = claudeClient;
        logger.info("OcrService initialized with OpenAI (primary) and Claude (fallback)");
    }

    /**
     * Extracts OCR data from a comic page image with OpenAI → Claude fallback.
     *
     * This method implements the resilient page-level processing pattern:
     * - Try OpenAI first (primary provider)
     * - On OpenAI failure, fallback to Claude (secondary provider)
     * - On both failures, return failed result with error message
     *
     * IMPORTANT: This method NEVER throws exceptions. All errors are captured
     * in PageOcrResult.failed() to ensure TTS jobs can complete even with failed pages.
     *
     * @param imageBytes raw image data (JPEG/PNG format)
     * @param pageNumber page number in the comic (1-indexed)
     * @return PageOcrResult containing either success data or failure information
     */
    public PageOcrResult extractPage(byte[] imageBytes, int pageNumber) {
        if (imageBytes == null || imageBytes.length == 0) {
            logger.error("Image bytes null or empty for page {}", pageNumber);
            return PageOcrResult.failed(pageNumber, "Invalid image data");
        }

        logger.info("Starting OCR cascade for page {}: OpenAI → Claude → Failed", pageNumber);

        // Try OpenAI first (primary provider)
        try {
            logger.info("Attempting OCR for page {} with OpenAI (primary)", pageNumber);
            OcrResponse response = openAiClient.ocrExtract(imageBytes);

            logger.info("OpenAI succeeded for page {}: {} segments, {} tokens",
                       pageNumber, response.getSegments().size(), response.getTokensUsed());

            PageOcrResult result = buildSuccessResult(response, pageNumber);
            logger.info("Provider cascade result for page {}: OpenAI SUCCESS", pageNumber);
            return result;

        } catch (OcrException e) {
            logger.warn("OpenAI failed for page {} after all retries: {}", pageNumber, e.getMessage());

            // Fallback to Claude (secondary provider)
            try {
                logger.info("Attempting OCR for page {} with Claude (fallback)", pageNumber);
                OcrResponse response = claudeClient.ocrExtract(imageBytes);

                logger.info("Claude succeeded for page {}: {} segments, {} tokens",
                           pageNumber, response.getSegments().size(), response.getTokensUsed());

                PageOcrResult result = buildSuccessResult(response, pageNumber);
                logger.info("Provider cascade result for page {}: Claude SUCCESS (fallback)", pageNumber);
                return result;

            } catch (OcrException ce) {
                // Both providers failed - log and return failed result
                logger.error("Both OCR providers failed for page {}: OpenAI={}, Claude={}",
                           pageNumber, e.getMessage(), ce.getMessage());

                String errorMessage = String.format(
                    "OCR failed: OpenAI=%s, Claude=%s",
                    e.getMessage(),
                    ce.getMessage()
                );

                logger.info("Provider cascade result for page {}: FAILED", pageNumber);
                return PageOcrResult.failed(pageNumber, errorMessage);
            }
        }
    }

    /**
     * Builds a successful PageOcrResult from OcrResponse with voice characteristics inference.
     *
     * This method transforms the normalized OcrResponse (from OpenAI or Claude) into
     * a PageOcrResult with additional metadata including inferred voice characteristics
     * for each speaker segment.
     *
     * @param ocrResponse the OCR response from the provider
     * @param pageNumber the page number
     * @return PageOcrResult with success=true and voice characteristics
     */
    private PageOcrResult buildSuccessResult(OcrResponse ocrResponse, int pageNumber) {
        List<SpeakerSegment> segments = new ArrayList<>();

        for (OcrResponse.DialogueSegment dialogueSegment : ocrResponse.getSegments()) {
            VoiceCharacteristics voiceChars = inferVoiceCharacteristics(
                dialogueSegment.getSpeaker(),
                dialogueSegment.getText()
            );

            SpeakerSegment segment = new SpeakerSegment(
                dialogueSegment.getSpeaker(),
                dialogueSegment.getText(),
                voiceChars
            );
            segments.add(segment);
        }

        return PageOcrResult.success(
            pageNumber,
            ocrResponse.getProvider(),
            segments,
            ocrResponse.getTokensUsed()
        );
    }

    /**
     * Infers voice characteristics from speaker name and dialogue text.
     *
     * This is an MVP implementation using simple heuristics. Future enhancements
     * could use ML models or additional LLM calls for more accurate inference.
     *
     * @param speaker the speaker name (e.g., "Batman", "Wonder Woman", "Narrator")
     * @param text the dialogue text
     * @return VoiceCharacteristics with inferred gender, age, and emotion
     */
    private VoiceCharacteristics inferVoiceCharacteristics(String speaker, String text) {
        VoiceCharacteristics.Gender gender = inferGender(speaker);
        VoiceCharacteristics.Age age = inferAge(speaker);
        VoiceCharacteristics.Emotion emotion = inferEmotion(text);

        logger.debug("Inferred voice for '{}': gender={}, age={}, emotion={}",
                    speaker, gender, age, emotion);

        return new VoiceCharacteristics(gender, age, emotion);
    }

    /**
     * Infers gender from speaker name using pattern matching.
     *
     * Priority order:
     * 1. Explicit gender indicators (man/woman, boy/girl)
     * 2. Known character names (Batman, Wonder Woman)
     * 3. Title patterns (Mr./Mrs./Miss/Sir/Lady)
     * 4. Default to NEUTRAL if unable to determine
     *
     * @param speaker the speaker name
     * @return inferred Gender enum value
     */
    private VoiceCharacteristics.Gender inferGender(String speaker) {
        if (speaker == null || speaker.isEmpty()) {
            return VoiceCharacteristics.Gender.NEUTRAL;
        }

        String lowerSpeaker = speaker.toLowerCase();

        // Known male character patterns
        if (lowerSpeaker.matches(".*(man|boy|mr|batman|superman|spiderman|ironman|hulk|thor|" +
                                "joker|father|dad|sir|lord|king|prince|hero|dude|guy|bro).*")) {
            return VoiceCharacteristics.Gender.MALE;
        }

        // Known female character patterns
        if (lowerSpeaker.matches(".*(woman|girl|ms|mrs|miss|lady|batwoman|supergirl|wonderwoman|" +
                                "catwoman|mother|mom|queen|princess|heroine|sis|gal).*")) {
            return VoiceCharacteristics.Gender.FEMALE;
        }

        // Narrator default
        if (lowerSpeaker.contains("narrator") || lowerSpeaker.contains("narration") ||
            lowerSpeaker.contains("caption")) {
            return VoiceCharacteristics.Gender.NEUTRAL;
        }

        // Default to neutral if unable to determine
        return VoiceCharacteristics.Gender.NEUTRAL;
    }

    /**
     * Infers age from speaker name using pattern matching.
     *
     * Priority order:
     * 1. Explicit age indicators (kid, teen, young, old, elder)
     * 2. Youth-associated words (boy/girl vs man/woman)
     * 3. Default to ADULT
     *
     * @param speaker the speaker name
     * @return inferred Age enum value
     */
    private VoiceCharacteristics.Age inferAge(String speaker) {
        if (speaker == null || speaker.isEmpty()) {
            return VoiceCharacteristics.Age.ADULT;
        }

        String lowerSpeaker = speaker.toLowerCase();

        // Young patterns
        if (lowerSpeaker.matches(".*(boy|girl|kid|child|teen|young|youth|junior|baby|toddler|" +
                                "robin|superboy|supergirl|batgirl).*")) {
            return VoiceCharacteristics.Age.YOUNG;
        }

        // Elderly patterns
        if (lowerSpeaker.matches(".*(old|elder|elderly|ancient|senior|grandpa|grandma|grandfather|" +
                                "grandmother|gramps|granny|aged).*")) {
            return VoiceCharacteristics.Age.ELDERLY;
        }

        // Default to adult
        return VoiceCharacteristics.Age.ADULT;
    }

    /**
     * Infers emotion from dialogue text using punctuation and keyword analysis.
     *
     * Priority order:
     * 1. Punctuation patterns (!! = angry, ! = excited, ... = sad, ? = curious)
     * 2. Keyword matching (aggressive words = angry, positive words = excited)
     * 3. Default to NEUTRAL
     *
     * Note: This is a simple heuristic approach. More sophisticated sentiment analysis
     * could be added in future iterations.
     *
     * @param text the dialogue text
     * @return inferred Emotion enum value
     */
    private VoiceCharacteristics.Emotion inferEmotion(String text) {
        if (text == null || text.isEmpty()) {
            return VoiceCharacteristics.Emotion.NEUTRAL;
        }

        String lowerText = text.toLowerCase();

        // Angry patterns (multiple exclamation marks, aggressive words)
        if (text.contains("!!") || lowerText.matches(".*(damn|hell|die|kill|hate|angry|furious|" +
                                                     "rage|idiot|fool|stupid|shut up|get out).*")) {
            return VoiceCharacteristics.Emotion.ANGRY;
        }

        // Excited patterns (exclamation marks with positive words)
        if (text.contains("!") && lowerText.matches(".*(wow|amazing|great|awesome|fantastic|" +
                                                    "yes|yeah|hooray|excellent|incredible|" +
                                                    "wonderful|perfect|brilliant).*")) {
            return VoiceCharacteristics.Emotion.EXCITED;
        }

        // Sad patterns (ellipsis, melancholy words)
        if (text.contains("...") || lowerText.matches(".*(sad|sorry|cry|tears|weep|sob|miss|" +
                                                      "lost|gone|death|died|goodbye|farewell|" +
                                                      "never|alone|empty|broken).*")) {
            return VoiceCharacteristics.Emotion.SAD;
        }

        // Curious/questioning patterns
        if (text.contains("?") && !text.contains("!!") && !text.contains("...")) {
            return VoiceCharacteristics.Emotion.CURIOUS;
        }

        // Default to neutral
        return VoiceCharacteristics.Emotion.NEUTRAL;
    }
}
