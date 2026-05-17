package com.react.spring.meta.metaset.dto;

public record MetaSetApiOperationDto(
        String code,
        String name,
        String operationType,
        String method,
        String endpoint,
        String responseMode,
        String description,
        Boolean enabled
) {}
