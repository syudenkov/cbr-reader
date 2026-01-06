package com.cbrviewer.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing the OCR extraction result for a single comic page.
 * Includes speaker segments with voice characteristics and success/failure status.
 *
 * This DTO is used in the TTS pipeline to track page-level OCR processing results,
 * including which provider succeeded (OpenAI or Claude) and voice characteristic
 * inference for each speaker segment.
 */
public class PageOcrResult {

    private int pageNumber;
    private boolean success;
    private String provider; // "openai" or "claude"
    private List<SpeakerSegment> segments;
    private int tokensUsed;
    private String errorMessage; // Populated only if success=false

    // Private constructor - use factory methods
    private PageOcrResult() {
        this.segments = new ArrayList<>();
    }

    /**
     * Factory method for successful OCR extraction.
     *
     * @param pageNumber the page number (1-indexed)
     * @param provider the provider that succeeded ("openai" or "claude")
     * @param segments the list of speaker segments extracted
     * @param tokensUsed total tokens consumed by the API call
     * @return PageOcrResult with success=true
     */
    public static PageOcrResult success(int pageNumber, String provider,
                                       List<SpeakerSegment> segments, int tokensUsed) {
        PageOcrResult result = new PageOcrResult();
        result.pageNumber = pageNumber;
        result.success = true;
        result.provider = provider;
        result.segments = segments != null ? segments : new ArrayList<>();
        result.tokensUsed = tokensUsed;
        return result;
    }

    /**
     * Factory method for failed OCR extraction.
     * Used when both OpenAI and Claude providers fail after all retries.
     *
     * @param pageNumber the page number (1-indexed)
     * @param errorMessage descriptive error message
     * @return PageOcrResult with success=false
     */
    public static PageOcrResult failed(int pageNumber, String errorMessage) {
        PageOcrResult result = new PageOcrResult();
        result.pageNumber = pageNumber;
        result.success = false;
        result.errorMessage = errorMessage;
        return result;
    }

    // Getters
    public int getPageNumber() {
        return pageNumber;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getProvider() {
        return provider;
    }

    public List<SpeakerSegment> getSegments() {
        return segments;
    }

    public int getTokensUsed() {
        return tokensUsed;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Inner class representing a speaker's dialogue segment with voice characteristics.
     * Each segment corresponds to one speech bubble or narration box in the comic panel.
     */
    public static class SpeakerSegment {
        private String speaker;
        private String text;
        private VoiceCharacteristics voiceCharacteristics;

        public SpeakerSegment() {
        }

        public SpeakerSegment(String speaker, String text, VoiceCharacteristics voiceCharacteristics) {
            this.speaker = speaker;
            this.text = text;
            this.voiceCharacteristics = voiceCharacteristics;
        }

        public String getSpeaker() {
            return speaker;
        }

        public void setSpeaker(String speaker) {
            this.speaker = speaker;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public VoiceCharacteristics getVoiceCharacteristics() {
            return voiceCharacteristics;
        }

        public void setVoiceCharacteristics(VoiceCharacteristics voiceCharacteristics) {
            this.voiceCharacteristics = voiceCharacteristics;
        }

        @Override
        public String toString() {
            return String.format("SpeakerSegment{speaker='%s', text='%s', voice=%s}",
                               speaker, text, voiceCharacteristics);
        }
    }

    /**
     * Voice characteristics inferred from speaker name and dialogue text.
     * Used for TTS voice mapping to select appropriate voice profiles.
     *
     * Inference is done using simple heuristics in MVP:
     * - Gender: based on character name patterns (man/woman, boy/girl, known characters)
     * - Age: based on age indicators in name (kid, teen, old, elder)
     * - Emotion: based on text punctuation and keywords (!! = angry, ! = excited, ... = sad)
     */
    public static class VoiceCharacteristics {

        public enum Gender {
            MALE,
            FEMALE,
            NEUTRAL
        }

        public enum Age {
            YOUNG,
            ADULT,
            ELDERLY
        }

        public enum Emotion {
            NEUTRAL,
            ANGRY,
            EXCITED,
            SAD,
            CURIOUS
        }

        private Gender gender;
        private Age age;
        private Emotion emotion;

        public VoiceCharacteristics() {
        }

        public VoiceCharacteristics(Gender gender, Age age, Emotion emotion) {
            this.gender = gender;
            this.age = age;
            this.emotion = emotion;
        }

        public Gender getGender() {
            return gender;
        }

        public void setGender(Gender gender) {
            this.gender = gender;
        }

        public Age getAge() {
            return age;
        }

        public void setAge(Age age) {
            this.age = age;
        }

        public Emotion getEmotion() {
            return emotion;
        }

        public void setEmotion(Emotion emotion) {
            this.emotion = emotion;
        }

        @Override
        public String toString() {
            return String.format("Voice{gender=%s, age=%s, emotion=%s}", gender, age, emotion);
        }
    }

    @Override
    public String toString() {
        if (success) {
            return String.format("PageOcrResult{page=%d, success=true, provider='%s', segments=%d, tokens=%d}",
                               pageNumber, provider, segments.size(), tokensUsed);
        } else {
            return String.format("PageOcrResult{page=%d, success=false, error='%s'}",
                               pageNumber, errorMessage);
        }
    }
}
