package com.cbrviewer.dto;

/**
 * Request DTO for creating a TTS job.
 * Used in POST /api/tts/jobs endpoint.
 */
public class TtsJobRequest {

    private Long fileId;

    public TtsJobRequest() {
    }

    public TtsJobRequest(Long fileId) {
        this.fileId = fileId;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }
}
