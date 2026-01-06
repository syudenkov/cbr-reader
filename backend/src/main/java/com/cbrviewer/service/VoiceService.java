package com.cbrviewer.service;

import com.cbrviewer.dto.PageOcrResult.VoiceCharacteristics;
import com.cbrviewer.dto.PageOcrResult.VoiceCharacteristics.Gender;
import com.cbrviewer.dto.PageOcrResult.VoiceCharacteristics.Age;
import com.cbrviewer.dto.PageOcrResult.VoiceCharacteristics.Emotion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for mapping voice characteristics to Minimax TTS voice IDs.
 * Implements deterministic voice mapping: same characteristics always return same voice ID.
 *
 * Voice mapping is hardcoded for MVP. Future iterations may fetch voice inventory
 * dynamically from Minimax API.
 */
@Service
public class VoiceService {

    private static final Logger logger = LoggerFactory.getLogger(VoiceService.class);
    private static final String DEFAULT_NARRATOR_VOICE = "narrator_neutral";

    // Voice mapping lookup table (hardcoded for MVP)
    private static final Map<String, String> VOICE_MAP = new HashMap<>();

    static {
        // Male voices
        VOICE_MAP.put("MALE-YOUNG-NEUTRAL", "hero_young_male");
        VOICE_MAP.put("MALE-YOUNG-EXCITED", "hero_young_male");
        VOICE_MAP.put("MALE-YOUNG-CURIOUS", "hero_young_male");
        VOICE_MAP.put("MALE-YOUNG-ANGRY", "villain_young_male");
        VOICE_MAP.put("MALE-YOUNG-SAD", "hero_young_male");

        VOICE_MAP.put("MALE-ADULT-NEUTRAL", "narrator_deep_male");
        VOICE_MAP.put("MALE-ADULT-ANGRY", "villain_deep_male");
        VOICE_MAP.put("MALE-ADULT-EXCITED", "hero_deep_male");
        VOICE_MAP.put("MALE-ADULT-CURIOUS", "narrator_deep_male");
        VOICE_MAP.put("MALE-ADULT-SAD", "narrator_deep_male");

        VOICE_MAP.put("MALE-ELDERLY-NEUTRAL", "mentor_old_male");
        VOICE_MAP.put("MALE-ELDERLY-ANGRY", "mentor_old_male");
        VOICE_MAP.put("MALE-ELDERLY-EXCITED", "mentor_old_male");
        VOICE_MAP.put("MALE-ELDERLY-CURIOUS", "mentor_old_male");
        VOICE_MAP.put("MALE-ELDERLY-SAD", "mentor_old_male");

        // Female voices
        VOICE_MAP.put("FEMALE-YOUNG-NEUTRAL", "heroine_young_female");
        VOICE_MAP.put("FEMALE-YOUNG-EXCITED", "heroine_young_female");
        VOICE_MAP.put("FEMALE-YOUNG-CURIOUS", "heroine_young_female");
        VOICE_MAP.put("FEMALE-YOUNG-ANGRY", "villainess_young_female");
        VOICE_MAP.put("FEMALE-YOUNG-SAD", "heroine_young_female");

        VOICE_MAP.put("FEMALE-ADULT-NEUTRAL", "narrator_female");
        VOICE_MAP.put("FEMALE-ADULT-ANGRY", "villainess_female");
        VOICE_MAP.put("FEMALE-ADULT-EXCITED", "heroine_female");
        VOICE_MAP.put("FEMALE-ADULT-CURIOUS", "narrator_female");
        VOICE_MAP.put("FEMALE-ADULT-SAD", "narrator_female");

        VOICE_MAP.put("FEMALE-ELDERLY-NEUTRAL", "mentor_old_female");
        VOICE_MAP.put("FEMALE-ELDERLY-ANGRY", "mentor_old_female");
        VOICE_MAP.put("FEMALE-ELDERLY-EXCITED", "mentor_old_female");
        VOICE_MAP.put("FEMALE-ELDERLY-CURIOUS", "mentor_old_female");
        VOICE_MAP.put("FEMALE-ELDERLY-SAD", "mentor_old_female");

        // Neutral voices (narrator default)
        VOICE_MAP.put("NEUTRAL-ADULT-NEUTRAL", DEFAULT_NARRATOR_VOICE);
        VOICE_MAP.put("NEUTRAL-ADULT-ANGRY", DEFAULT_NARRATOR_VOICE);
        VOICE_MAP.put("NEUTRAL-ADULT-EXCITED", DEFAULT_NARRATOR_VOICE);
        VOICE_MAP.put("NEUTRAL-ADULT-CURIOUS", DEFAULT_NARRATOR_VOICE);
        VOICE_MAP.put("NEUTRAL-ADULT-SAD", DEFAULT_NARRATOR_VOICE);

        VOICE_MAP.put("NEUTRAL-YOUNG-NEUTRAL", DEFAULT_NARRATOR_VOICE);
        VOICE_MAP.put("NEUTRAL-YOUNG-ANGRY", DEFAULT_NARRATOR_VOICE);
        VOICE_MAP.put("NEUTRAL-YOUNG-EXCITED", DEFAULT_NARRATOR_VOICE);
        VOICE_MAP.put("NEUTRAL-YOUNG-CURIOUS", DEFAULT_NARRATOR_VOICE);
        VOICE_MAP.put("NEUTRAL-YOUNG-SAD", DEFAULT_NARRATOR_VOICE);

        VOICE_MAP.put("NEUTRAL-ELDERLY-NEUTRAL", DEFAULT_NARRATOR_VOICE);
        VOICE_MAP.put("NEUTRAL-ELDERLY-ANGRY", DEFAULT_NARRATOR_VOICE);
        VOICE_MAP.put("NEUTRAL-ELDERLY-EXCITED", DEFAULT_NARRATOR_VOICE);
        VOICE_MAP.put("NEUTRAL-ELDERLY-CURIOUS", DEFAULT_NARRATOR_VOICE);
        VOICE_MAP.put("NEUTRAL-ELDERLY-SAD", DEFAULT_NARRATOR_VOICE);

        logger.info("Voice mapping table initialized with {} entries", VOICE_MAP.size());
    }

    /**
     * Maps voice characteristics to a Minimax voice ID.
     * Mapping is deterministic: same characteristics always return same voice ID.
     *
     * Mapping strategy:
     * 1. Try exact match: GENDER-AGE-EMOTION
     * 2. Fallback to neutral emotion: GENDER-AGE-NEUTRAL
     * 3. Final fallback: narrator_neutral
     *
     * @param characteristics voice characteristics from OCR inference (can be null)
     * @return Minimax voice ID string (e.g., "hero_deep_male", "narrator_neutral")
     */
    public String mapVoiceCharacteristics(VoiceCharacteristics characteristics) {
        // Handle null characteristics
        if (characteristics == null) {
            logger.warn("Voice characteristics null, using default narrator voice");
            return DEFAULT_NARRATOR_VOICE;
        }

        // Extract characteristics with defaults
        Gender gender = characteristics.getGender() != null ?
            characteristics.getGender() : Gender.NEUTRAL;
        Age age = characteristics.getAge() != null ?
            characteristics.getAge() : Age.ADULT;
        Emotion emotion = characteristics.getEmotion() != null ?
            characteristics.getEmotion() : Emotion.NEUTRAL;

        // Build lookup key: "GENDER-AGE-EMOTION"
        String key = String.format("%s-%s-%s", gender, age, emotion);

        // Try exact match first
        String voiceId = VOICE_MAP.get(key);
        if (voiceId != null) {
            logger.debug("Voice mapping: {} → {}", key, voiceId);
            return voiceId;
        }

        // Fallback: try without emotion (neutral emotion)
        String fallbackKey = String.format("%s-%s-NEUTRAL", gender, age);
        voiceId = VOICE_MAP.get(fallbackKey);
        if (voiceId != null) {
            logger.debug("Voice mapping (fallback to NEUTRAL): {} → {}", fallbackKey, voiceId);
            return voiceId;
        }

        // Final fallback: neutral narrator
        logger.warn("No voice mapping found for {}, using default narrator", key);
        return DEFAULT_NARRATOR_VOICE;
    }

    /**
     * Returns the complete voice inventory (lookup table).
     * Returns an unmodifiable map to prevent external modification.
     *
     * @return unmodifiable map of voice mappings (GENDER-AGE-EMOTION → voice ID)
     */
    public Map<String, String> getVoiceInventory() {
        return Collections.unmodifiableMap(VOICE_MAP);
    }

    /**
     * Refreshes voice inventory from Minimax API.
     * NOT IMPLEMENTED in MVP - voice mapping is hardcoded.
     * Future enhancement: fetch available voices dynamically from Minimax API
     * and update VOICE_MAP accordingly.
     */
    public void refreshVoiceInventory() {
        logger.warn("Voice inventory refresh not implemented in MVP (hardcoded map)");
        // TODO: Call Minimax API to fetch available voices
        // TODO: Update VOICE_MAP dynamically based on API response
    }
}
