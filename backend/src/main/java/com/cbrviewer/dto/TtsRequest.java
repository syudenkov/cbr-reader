package com.cbrviewer.dto;

/**
 * DTO representing a Text-to-Speech synthesis request to Minimax API.
 * Used internally by MinimaxClient to build API request payloads.
 *
 * Request format matches Minimax API specification:
 * POST https://api.minimax.chat/v1/tts
 * {
 *   "text": "dialogue text",
 *   "voice": "voice_id",
 *   "format": "mp3"
 * }
 */
public class TtsRequest {

    private String text;
    private String voice;
    private String format;

    /**
     * Default constructor. Sets format to "mp3" by default.
     */
    public TtsRequest() {
        this.format = "mp3"; // Default format
    }

    /**
     * Convenience constructor for creating TTS requests.
     *
     * @param text the dialogue text to synthesize
     * @param voice the Minimax voice ID (e.g., "hero_deep_male")
     */
    public TtsRequest(String text, String voice) {
        this.text = text;
        this.voice = voice;
        this.format = "mp3";
    }

    // Getters and setters

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getVoice() {
        return voice;
    }

    public void setVoice(String voice) {
        this.voice = voice;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    public String toString() {
        return String.format("TtsRequest{voice='%s', format='%s', textLen=%d}",
                           voice, format, text != null ? text.length() : 0);
    }
}
