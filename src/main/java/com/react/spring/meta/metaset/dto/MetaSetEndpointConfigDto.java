package com.react.spring.meta.metaset.dto;

public record MetaSetEndpointConfigDto(
        String basePath,
        String primaryOperationCode,
        String primaryOperationType
) {}
