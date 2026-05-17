package com.react.spring.meta.metasetversion.entity;

import com.react.spring.common.entity.BaseEntity;
import com.react.spring.meta.metaset.entity.MetaSetApiSetting;
import com.react.spring.meta.metaset.entity.MetaSetOperation;
import com.vn.security.core.security.catalog.SecuredEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SecuredEntity(jpqlAllowed = true)
@Entity
@Table(name = "core_meta_set_version", uniqueConstraints = {
        @UniqueConstraint(
                name = "uq_core_meta_set_version_code_no",
                columnNames = {"meta_code", "version_no"}
        )
})
public class MetaSetVersion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    // Code của MetaSource (copy denormalized — KHÔNG khai báo relation, chạy độc lập).
    @Column(name = "data_source_code", length = 200)
    private String dataSourceCode;

    // Code của MetaSet (copy denormalized — KHÔNG khai báo relation, chạy độc lập).
    @Column(name = "meta_code", length = 200, nullable = false)
    private String metaCode;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    // Code của MetaSync (copy denormalized — KHÔNG khai báo relation, chạy độc lập).
    @Column(name = "metasync_code", length = 200)
    private String metasyncCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "field_data", columnDefinition = "jsonb")
    private String fieldData;

    @Column(name = "field_hash", length = 128)
    private String fieldHash;

    @Column(name = "example_data", columnDefinition = "TEXT")
    private String exampleData;

    @Column(name = "endpoint_path", length = 500)
    private String endpointPath;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "endpoint_config", columnDefinition = "jsonb")
    private String endpointConfig;

    @OneToOne(mappedBy = "metaSetVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    private MetaSetApiSetting apiSetting;

    @OneToMany(mappedBy = "metaSetVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<MetaSetOperation> operations = new ArrayList<>();

    // Cờ đánh dấu version này đại diện cho thao tác xoá schema item (cho diff engine).
    // KHÁC `deleted_date` của BaseEntity (soft-delete của hàng).
    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted = Boolean.FALSE;

    @Column(name = "changed_status", columnDefinition = "TEXT", nullable = false)
    private String changedStatus;

    @Column(name = "changed_summary", columnDefinition = "TEXT")
    private String changedSummary;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getDataSourceCode() { return dataSourceCode; }
    public void setDataSourceCode(String dataSourceCode) { this.dataSourceCode = dataSourceCode; }

    public String getMetaCode() { return metaCode; }
    public void setMetaCode(String metaCode) { this.metaCode = metaCode; }

    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }

    public String getMetasyncCode() { return metasyncCode; }
    public void setMetasyncCode(String metasyncCode) { this.metasyncCode = metasyncCode; }

    public String getFieldData() { return fieldData; }
    public void setFieldData(String fieldData) { this.fieldData = fieldData; }

    public String getFieldHash() { return fieldHash; }
    public void setFieldHash(String fieldHash) { this.fieldHash = fieldHash; }

    public String getExampleData() { return exampleData; }
    public void setExampleData(String exampleData) { this.exampleData = exampleData; }

    public String getEndpointPath() { return endpointPath; }
    public void setEndpointPath(String endpointPath) { this.endpointPath = endpointPath; }

    public String getEndpointConfig() { return endpointConfig; }
    public void setEndpointConfig(String endpointConfig) { this.endpointConfig = endpointConfig; }

    public MetaSetApiSetting getApiSetting() { return apiSetting; }
    public void setApiSetting(MetaSetApiSetting apiSetting) { this.apiSetting = apiSetting; }

    public List<MetaSetOperation> getOperations() { return operations; }
    public void setOperations(List<MetaSetOperation> operations) { this.operations = operations; }

    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }

    public String getChangedStatus() { return changedStatus; }
    public void setChangedStatus(String changedStatus) { this.changedStatus = changedStatus; }

    public String getChangedSummary() { return changedSummary; }
    public void setChangedSummary(String changedSummary) { this.changedSummary = changedSummary; }
}
