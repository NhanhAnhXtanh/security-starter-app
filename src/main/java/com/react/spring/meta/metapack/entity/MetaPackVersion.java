package com.react.spring.meta.metapack.entity;

import com.react.spring.common.entity.BaseEntity;
import com.vn.security.core.security.catalog.SecuredEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@SecuredEntity(jpqlAllowed = true)
@Entity
@Table(name = "meta_pack_version")
public class MetaPackVersion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meta_pack_id", nullable = false)
    private MetaPack metaPack;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber = 1;

    @Column(name = "status")
    private String status;

    @Column(name = "release_notes", columnDefinition = "TEXT")
    private String releaseNotes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_data", columnDefinition = "jsonb")
    private String dataConfig;

    @Column(name = "data_hash", length = 128)
    private String dataHash;

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public MetaPack getMetaPack() { return metaPack; }
    public void setMetaPack(MetaPack metaPack) { this.metaPack = metaPack; }

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
}
