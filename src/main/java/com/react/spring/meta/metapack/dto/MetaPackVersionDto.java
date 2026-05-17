package com.react.spring.meta.metapack.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class MetaPackVersionDto {
    private UUID id;
    private Integer versionNumber;
    private String status;
    private String releaseNotes;
    private String dataConfig;
    private String dataHash;
    private String createdBy;
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Integer getVersionNumber() { return versionNumber; }
    public void setVersionNumber(Integer versionNumber) { this.versionNumber = versionNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReleaseNotes() { return releaseNotes; }
    public void setReleaseNotes(String releaseNotes) { this.releaseNotes = releaseNotes; }

    public String getDataConfig() { return dataConfig; }
    public void setDataConfig(String dataConfig) { this.dataConfig = dataConfig; }

    public String getDataHash() { return dataHash; }
    public void setDataHash(String dataHash) { this.dataHash = dataHash; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
