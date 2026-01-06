package com.cbrviewer.dto;

import com.cbrviewer.model.ComicFile;

import java.time.LocalDateTime;

public class ComicFileResponse {

    private Long id;
    private String originalFilename;
    private String fileType;
    private Long fileSize;
    private Integer pageCount;
    private Long uploadedBy;
    private LocalDateTime createdAt;

    public ComicFileResponse() {
    }

    public ComicFileResponse(Long id, String originalFilename, String fileType, Long fileSize,
                             Integer pageCount, Long uploadedBy, LocalDateTime createdAt) {
        this.id = id;
        this.originalFilename = originalFilename;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.pageCount = pageCount;
        this.uploadedBy = uploadedBy;
        this.createdAt = createdAt;
    }

    public static ComicFileResponse fromEntity(ComicFile file) {
        return new ComicFileResponse(
            file.getId(),
            file.getOriginalFilename(),
            file.getFileType(),
            file.getFileSize(),
            file.getPageCount(),
            file.getUploadedBy(),
            file.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }

    public Long getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(Long uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
