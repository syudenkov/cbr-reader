package com.cbrviewer.dto;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for TTS job status responses.
 * Contains job progress information for polling by frontend.
 */
public class TtsJobDto {

    private Long jobId;
    private Long fileId;
    private String status;
    private Integer progress;
    private Integer currentPage;
    private Integer totalPages;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TtsJobDto() {
    }

    public TtsJobDto(
        Long jobId,
        Long fileId,
        String status,
        Integer progress,
        Integer currentPage,
        Integer totalPages,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        this.jobId = jobId;
        this.fileId = fileId;
        this.status = status;
        this.progress = progress;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public Integer getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
