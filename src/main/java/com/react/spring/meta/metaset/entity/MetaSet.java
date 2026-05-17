package com.react.spring.meta.metaset.entity;

import com.react.spring.common.entity.BaseEntity;
import com.react.spring.catalog.entity.Domain;
import com.react.spring.catalog.entity.Organization;
import com.react.spring.meta.metasetversion.entity.MetaSetVersion;
import com.react.spring.meta.metasource.entity.MetaSource;
import com.vn.security.core.security.catalog.SecuredEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@SecuredEntity(jpqlAllowed = true)
@Entity
@Table(name = "core_meta_set")
public class MetaSet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", length = 200, nullable = false)
    private String code;

    // Tên bảng / mã bảng gốc (nếu được trích xuất từ MetaSync/MetaSource)
    @Column(name = "meta_code", length = 200)
    private String metaCode;

    @Column(name = "name", length = 500)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "data_source_id", nullable = false)
    private MetaSource metaSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id")
    private Domain domain;

    @Column(name = "classification", length = 50)
    private String classification;

    @Column(name = "tier", length = 50)
    private String tier;

    @Column(name = "status", length = 50, nullable = false)
    private String status = "DRAFT";

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "published_by", length = 200)
    private String publishedBy;

    @Column(name = "published_comment", length = 1000)
    private String publishedComment;

    @Column(name = "discontinued_at")
    private OffsetDateTime discontinuedAt;

    @Column(name = "discontinued_by", length = 200)
    private String discontinuedBy;

    @Column(name = "discontinued_comment", length = 1000)
    private String discontinuedComment;

    @Column(name = "last_synced_at")
    private OffsetDateTime lastSyncedAt;

    @Column(name = "last_sync_status", length = 50)
    private String lastSyncStatus;

    @Column(name = "last_synced_version")
    private Integer lastSyncedVersion;

    // Quan hệ 1-1: mỗi MetaSet có một MetaSetVersion hiện hành; tạo cùng lúc với MetaSet.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_version_id")
    private MetaSetVersion currentVersion;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "core_meta_set_tag",
            joinColumns = @JoinColumn(name = "meta_set_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private java.util.Set<com.react.spring.catalog.entity.Tag> tags;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getMetaCode() { return metaCode; }
    public void setMetaCode(String metaCode) { this.metaCode = metaCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public MetaSource getMetaSource() { return metaSource; }
    public void setMetaSource(MetaSource metaSource) { this.metaSource = metaSource; }

    public Organization getOrganization() { return organization; }
    public void setOrganization(Organization organization) { this.organization = organization; }

    public Domain getDomain() { return domain; }
    public void setDomain(Domain domain) { this.domain = domain; }

    public String getClassification() { return classification; }
    public void setClassification(String classification) { this.classification = classification; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public OffsetDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(OffsetDateTime publishedAt) { this.publishedAt = publishedAt; }

    public String getPublishedBy() { return publishedBy; }
    public void setPublishedBy(String publishedBy) { this.publishedBy = publishedBy; }

    public String getPublishedComment() { return publishedComment; }
    public void setPublishedComment(String publishedComment) { this.publishedComment = publishedComment; }

    public OffsetDateTime getDiscontinuedAt() { return discontinuedAt; }
    public void setDiscontinuedAt(OffsetDateTime discontinuedAt) { this.discontinuedAt = discontinuedAt; }

    public String getDiscontinuedBy() { return discontinuedBy; }
    public void setDiscontinuedBy(String discontinuedBy) { this.discontinuedBy = discontinuedBy; }

    public String getDiscontinuedComment() { return discontinuedComment; }
    public void setDiscontinuedComment(String discontinuedComment) { this.discontinuedComment = discontinuedComment; }

    public OffsetDateTime getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(OffsetDateTime lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }

    public String getLastSyncStatus() { return lastSyncStatus; }
    public void setLastSyncStatus(String lastSyncStatus) { this.lastSyncStatus = lastSyncStatus; }

    public Integer getLastSyncedVersion() { return lastSyncedVersion; }
    public void setLastSyncedVersion(Integer lastSyncedVersion) { this.lastSyncedVersion = lastSyncedVersion; }

    public MetaSetVersion getCurrentVersion() { return currentVersion; }
    public void setCurrentVersion(MetaSetVersion currentVersion) { this.currentVersion = currentVersion; }

    public java.util.Set<com.react.spring.catalog.entity.Tag> getTags() { return tags; }
    public void setTags(java.util.Set<com.react.spring.catalog.entity.Tag> tags) { this.tags = tags; }
}
