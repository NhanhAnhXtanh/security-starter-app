package com.react.spring.meta.metaset.entity;

import com.react.spring.common.entity.BaseEntity;
import com.react.spring.meta.metaset.entity.dto.MetaSetApiSettingConfig;
import com.react.spring.meta.metaset.util.MetaSetApiSettingConfigConverter;
import com.react.spring.meta.metasetversion.entity.MetaSetVersion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "core_meta_set_api_setting")
public class MetaSetApiSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meta_set_version_id", unique = true)
    private MetaSetVersion metaSetVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "jsonb")
    private String config;

    @Transient
    private MetaSetApiSettingConfig configDto;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public MetaSetVersion getMetaSetVersion() { return metaSetVersion; }
    public void setMetaSetVersion(MetaSetVersion metaSetVersion) { this.metaSetVersion = metaSetVersion; }

    public String getConfig() { return config; }

    public void setConfig(String config) {
        this.config = config;
        this.configDto = null;
    }

    public MetaSetApiSettingConfig getConfigDto() {
        if (configDto == null) {
            configDto = MetaSetApiSettingConfigConverter.fromJson(config);
        }
        return configDto;
    }

    public void setConfigDto(MetaSetApiSettingConfig configDto) {
        this.configDto = configDto;
        this.config = MetaSetApiSettingConfigConverter.toJson(configDto);
    }
}
