package com.cbrviewer.dto;

import com.cbrviewer.model.ReadingProgress;

import java.time.LocalDateTime;

public class ReadingProgressResponse {

    private Long fileId;
    private Integer currentPage;
    private LocalDateTime updatedAt;

    public ReadingProgressResponse() {
    }

    public ReadingProgressResponse(Long fileId, Integer currentPage, LocalDateTime updatedAt) {
        this.fileId = fileId;
        this.currentPage = currentPage;
        this.updatedAt = updatedAt;
    }

    public static ReadingProgressResponse fromEntity(ReadingProgress progress) {
        return new ReadingProgressResponse(
            progress.getFileId(),
            progress.getCurrentPage(),
            progress.getUpdatedAt()
        );
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Integer getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
