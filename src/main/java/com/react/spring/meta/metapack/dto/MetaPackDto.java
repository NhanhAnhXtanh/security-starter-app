package com.react.spring.meta.metapack.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class MetaPackDto {
    private UUID id;
    private String code;
    private String name;
    private String description;
    private String status;
    private Integer maxRequestsPerMinute;
    private Integer maxRequestsPerDay;
    private UUID currentVersionId;
    private String createdBy;
    private OffsetDateTime createdAt;
    private String updatedBy;
    private OffsetDateTime updatedAt;
    private List<MetaPackVersionItemDto> versionItems;

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getMaxRequestsPerMinute() { return maxRequestsPerMinute; }
    public void setMaxRequestsPerMinute(Integer maxRequestsPerMinute) { this.maxRequestsPerMinute = maxRequestsPerMinute; }
    public Integer getMaxRequestsPerDay() { return maxRequestsPerDay; }
    public void setMaxRequestsPerDay(Integer maxRequestsPerDay) { this.maxRequestsPerDay = maxRequestsPerDay; }
    public UUID getCurrentVersionId() { return currentVersionId; }
    public void setCurrentVersionId(UUID currentVersionId) { this.currentVersionId = currentVersionId; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<MetaPackVersionItemDto> getVersionItems() { return versionItems; }
    public void setVersionItems(List<MetaPackVersionItemDto> versionItems) { this.versionItems = versionItems; }
}
