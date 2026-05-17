package com.react.spring.meta.metasource.dto;

import com.react.spring.common.enums.ConnectorType;
import com.react.spring.common.enums.SourceType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MetaSourceDto(
        UUID id,
        String code,
        String name,
        SourceType sourceType,
        ConnectorType connectorType,
        String description,
        Boolean enabled,
        UUID organizationId,
        String organizationName,
        UUID domainId,
        String domainName,
        String connectorConfig,
        OffsetDateTime createdDate,
        OffsetDateTime lastModifiedDate
) {}
