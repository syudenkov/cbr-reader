package com.cbrviewer.dto;

/**
 * DTO representing an OCR extraction request.
 * Used internally to encapsulate image data and metadata for OCR processing.
 */
public class OcrRequest {

    private byte[] imageBytes;
    private Integer pageNumber;
    private String provider;

    public OcrRequest() {
    }

    public OcrRequest(byte[] imageBytes, Integer pageNumber) {
        this.imageBytes = imageBytes;
        this.pageNumber = pageNumber;
    }

    public OcrRequest(byte[] imageBytes, Integer pageNumber, String provider) {
        this.imageBytes = imageBytes;
        this.pageNumber = pageNumber;
        this.provider = provider;
    }

    public byte[] getImageBytes() {
        return imageBytes;
    }

    public void setImageBytes(byte[] imageBytes) {
        this.imageBytes = imageBytes;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    @Override
    public String toString() {
        return String.format("OcrRequest{pageNumber=%d, provider='%s', imageSize=%d bytes}",
                           pageNumber, provider, imageBytes != null ? imageBytes.length : 0);
    }
}
