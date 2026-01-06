package com.cbrviewer.dto;

import com.cbrviewer.model.LlmConfig;

import java.time.LocalDateTime;

public class LlmConfigDto {

    private Long id;
    private String provider;
    private String modelName;
    private Integer isActive;
    private Integer priority;
    private String apiKeyMasked;
    private LocalDateTime updatedAt;

    public static LlmConfigDto fromLlmConfig(LlmConfig config, String maskedKey) {
        LlmConfigDto dto = new LlmConfigDto();
        dto.setId(config.getId());
        dto.setProvider(config.getProvider());
        dto.setModelName(config.getModelName());
        dto.setIsActive(config.getIsActive());
        dto.setPriority(config.getPriority());
        dto.setApiKeyMasked(maskedKey);
        dto.setUpdatedAt(config.getUpdatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getApiKeyMasked() {
        return apiKeyMasked;
    }

    public void setApiKeyMasked(String apiKeyMasked) {
        this.apiKeyMasked = apiKeyMasked;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
