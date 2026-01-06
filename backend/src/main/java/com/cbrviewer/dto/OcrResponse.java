package com.cbrviewer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing the OCR extraction result from an LLM provider.
 * Contains dialogue segments with speaker identification and token usage metadata.
 */
public class OcrResponse {

    private List<DialogueSegment> segments;
    private String provider;

    @JsonProperty("tokens_used")
    private int tokensUsed;

    public OcrResponse() {
        this.segments = new ArrayList<>();
    }

    public OcrResponse(List<DialogueSegment> segments, String provider, int tokensUsed) {
        this.segments = segments != null ? segments : new ArrayList<>();
        this.provider = provider;
        this.tokensUsed = tokensUsed;
    }

    public List<DialogueSegment> getSegments() {
        return segments;
    }

    public void setSegments(List<DialogueSegment> segments) {
        this.segments = segments;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public int getTokensUsed() {
        return tokensUsed;
    }

    public void setTokensUsed(int tokensUsed) {
        this.tokensUsed = tokensUsed;
    }

    /**
     * Inner class representing a single dialogue segment.
     * Maps to the JSON structure: {"speaker": "Character Name", "text": "Dialogue"}
     */
    public static class DialogueSegment {

        private String speaker;
        private String text;

        public DialogueSegment() {
        }

        public DialogueSegment(String speaker, String text) {
            this.speaker = speaker;
            this.text = text;
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

        @Override
        public String toString() {
            return String.format("DialogueSegment{speaker='%s', text='%s'}", speaker, text);
        }
    }

    @Override
    public String toString() {
        return String.format("OcrResponse{provider='%s', tokensUsed=%d, segments=%d}",
                           provider, tokensUsed, segments.size());
    }
}
