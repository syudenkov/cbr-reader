package com.cbrviewer.dto;

public class AverageRatingResponse {
    private Long fileId;
    private Double averageRating;
    private Integer totalRatings;

    public AverageRatingResponse() {}

    public AverageRatingResponse(Long fileId, Double averageRating, Integer totalRatings) {
        this.fileId = fileId;
        this.averageRating = averageRating;
        this.totalRatings = totalRatings;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getTotalRatings() {
        return totalRatings;
    }

    public void setTotalRatings(Integer totalRatings) {
        this.totalRatings = totalRatings;
    }
}
