package com.react.spring.meta.metaset.dto;

public record MetaSetApiAuthDto(
        String authType,
        String username,
        String password,
        String bearerToken,
        String apiKeyName,
        String apiKeyValue,
        String apiKeyPlacement
) {}
