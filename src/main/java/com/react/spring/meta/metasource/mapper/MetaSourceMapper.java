package com.react.spring.meta.metasource.mapper;

import com.react.spring.meta.metasource.dto.MetaSourceDto;
import com.react.spring.catalog.entity.Domain;
import com.react.spring.catalog.entity.Organization;
import com.react.spring.meta.metasource.entity.MetaSource;

public final class MetaSourceMapper {

    private MetaSourceMapper() {}

    public static MetaSourceDto toDto(MetaSource e) {
        Organization org = e.getOrganization();
        Domain dom = e.getDomain();
        return new MetaSourceDto(
                e.getId(),
                e.getCode(),
                e.getName(),
                e.getSourceType(),
                e.getConnectorType(),
                e.getDescription(),
                e.getEnabled(),
                org != null ? org.getId() : null,
                org != null ? org.getName() : null,
                dom != null ? dom.getId() : null,
                dom != null ? dom.getName() : null,
                e.getConnectorConfig(),
                e.getCreatedDate(),
                e.getLastModifiedDate()
        );
    }
}
