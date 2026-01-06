package com.cbrviewer.dto;

import com.cbrviewer.model.Rating;
import java.time.LocalDateTime;

public class RatingResponse {
    private Long id;
    private Long userId;
    private Long fileId;
    private Integer rating;
    private LocalDateTime createdAt;

    public RatingResponse() {}

    public RatingResponse(Long id, Long userId, Long fileId, Integer rating, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.fileId = fileId;
        this.rating = rating;
        this.createdAt = createdAt;
    }

    public static RatingResponse fromEntity(Rating rating) {
        return new RatingResponse(
            rating.getId(),
            rating.getUserId(),
            rating.getFileId(),
            rating.getRating(),
            rating.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
