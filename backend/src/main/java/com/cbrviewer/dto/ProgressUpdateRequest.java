package com.cbrviewer.dto;

import jakarta.validation.constraints.NotNull;

public class ProgressUpdateRequest {

    @NotNull(message = "Current page is required")
    private Integer currentPage;

    public ProgressUpdateRequest() {
    }

    public ProgressUpdateRequest(Integer currentPage) {
        this.currentPage = currentPage;
    }

    public Integer getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }
}
