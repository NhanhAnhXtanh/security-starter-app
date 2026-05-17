package com.react.spring.meta.metasource.connect.rest.dto;

import java.util.List;

public record RestProxyResultDto(
        int status,
        List<RestProxyHeaderDto> headers,
        String body,
        long durationMs
) {}
