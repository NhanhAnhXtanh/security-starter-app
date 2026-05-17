package com.react.spring.meta.metasync.entity;

import com.react.spring.common.entity.BaseEntity;
import com.react.spring.meta.metasource.entity.MetaSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "core_meta_sync", uniqueConstraints = {
        @UniqueConstraint(name = "uq_core_meta_sync_source_meta_code_version", columnNames = {"data_source_id", "meta_code", "version_no"})
})
public class MetaSync extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    // Code định danh của bản thân MetaSync (tự sinh từ slug nghiệp vụ).
    @Column(name = "code", columnDefinition = "TEXT", nullable = false)
    private String code;

    @Column(name = "status", length = 50)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_source_id")
    private MetaSource metaSource;

    // TODO: chưa xác định giá trị lưu vào trường này (drawSQL ghi VARCHAR(200), comment
    // "metasetcode laf identify theo cái gì đó"). Để String, refine khi nghiệp vụ rõ.
    @Column(name = "meta_code", length = 200)
    private String metaCode;

    // TODO: chưa xác định giá trị lưu vào trường này. Refine khi nghiệp vụ rõ.
    @Column(name = "meta_name", length = 500)
    private String metaName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "field_data", columnDefinition = "jsonb", nullable = false)
    private String fieldData;

    @Column(name = "field_hash", columnDefinition = "TEXT", nullable = false)
    private String fieldHash;

    // Cờ đánh dấu sync này đại diện cho thao tác xoá schema item (cho diff engine).
    // KHÁC `deleted_date` của BaseEntity (soft-delete của hàng).
    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted = Boolean.FALSE;

    @Column(name = "is_active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Column(name = "changed_status", columnDefinition = "TEXT", nullable = false)
    private String changedStatus;

    @Column(name = "changed_summary", columnDefinition = "TEXT", nullable = false)
    private String changedSummary;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public MetaSource getMetaSource() { return metaSource; }
    public void setMetaSource(MetaSource metaSource) { this.metaSource = metaSource; }

    public String getMetaCode() { return metaCode; }
    public void setMetaCode(String metaCode) { this.metaCode = metaCode; }

    public String getMetaName() { return metaName; }
    public void setMetaName(String metaName) { this.metaName = metaName; }

    public String getFieldData() { return fieldData; }
    public void setFieldData(String fieldData) { this.fieldData = fieldData; }

    public String getFieldHash() { return fieldHash; }
    public void setFieldHash(String fieldHash) { this.fieldHash = fieldHash; }

    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }

    public String getChangedStatus() { return changedStatus; }
    public void setChangedStatus(String changedStatus) { this.changedStatus = changedStatus; }

    public String getChangedSummary() { return changedSummary; }
    public void setChangedSummary(String changedSummary) { this.changedSummary = changedSummary; }
}
