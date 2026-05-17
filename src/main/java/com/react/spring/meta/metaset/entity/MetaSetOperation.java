package com.react.spring.meta.metaset.entity;

import com.react.spring.common.entity.BaseEntity;
import com.react.spring.meta.metaset.entity.dto.MetaSetOperationConfig;
import com.react.spring.meta.metaset.util.MetaSetOperationConfigConverter;
import com.react.spring.meta.metasetversion.entity.MetaSetVersion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "core_meta_set_operation")
public class MetaSetOperation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meta_set_version_id")
    private MetaSetVersion metaSetVersion;

    @Column(name = "code", length = 100)
    private String code;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "operation_type", length = 50, nullable = false)
    private String operationType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "jsonb")
    private String config;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Transient
    private MetaSetOperationConfig configDto;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public MetaSetVersion getMetaSetVersion() { return metaSetVersion; }
    public void setMetaSetVersion(MetaSetVersion metaSetVersion) { this.metaSetVersion = metaSetVersion; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }

    public String getConfig() { return config; }
    public void setConfig(String config) {
        this.config = config;
        this.configDto = null;
    }

    public MetaSetOperationConfig getConfigDto() {
        if (configDto == null) {
            configDto = MetaSetOperationConfigConverter.fromJson(config);
        }
        return configDto;
    }

    public void setConfigDto(MetaSetOperationConfig configDto) {
        this.configDto = configDto;
        this.config = MetaSetOperationConfigConverter.toJson(configDto);
    }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
