package com.react.spring.meta.metasource.dto;

import com.react.spring.common.enums.ConnectorType;
import com.react.spring.common.enums.SourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record MetaSourceRequest(
        @NotBlank @Size(max = 500) String name,
        @NotNull SourceType sourceType,
        @NotNull ConnectorType connectorType,
        @Size(max = 500) String description,
        Boolean enabled,
        UUID organizationId,
        UUID domainId,
        String connectorConfig
) {}
