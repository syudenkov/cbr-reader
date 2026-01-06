package com.cbrviewer.dto;

import java.time.LocalDateTime;

public class LlmConfigTestResult {

    private String provider;
    private boolean success;
    private String message;
    private Long latencyMs;
    private LocalDateTime testedAt;

    public LlmConfigTestResult() {
    }

    public LlmConfigTestResult(String provider, boolean success, String message, Long latencyMs, LocalDateTime testedAt) {
        this.provider = provider;
        this.success = success;
        this.message = message;
        this.latencyMs = latencyMs;
        this.testedAt = testedAt;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public LocalDateTime getTestedAt() {
        return testedAt;
    }

    public void setTestedAt(LocalDateTime testedAt) {
        this.testedAt = testedAt;
    }
}
