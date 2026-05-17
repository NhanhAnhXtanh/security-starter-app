package com.react.spring.meta.metaset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record MetaSetRequest(
        @NotBlank @Size(max = 500) String name,
        @Size(max = 200) String metaCode,
        String description,
        @NotNull UUID metaSourceId,
        UUID organizationId,
        UUID domainId,
        @Size(max = 50) String classification,
        @Size(max = 50) String tier,
        String exampleData,
        @Size(max = 500) String endpointPath,
        MetaSetEndpointConfigDto endpointConfig,
        MetaSetApiSettingDto apiSetting,
        java.util.List<MetaSetApiOperationDto> operations,
        String fieldData,
        java.util.Set<UUID> tagIds
) {}
