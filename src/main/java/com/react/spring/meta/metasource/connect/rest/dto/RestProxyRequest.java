package com.react.spring.meta.metasource.connect.rest.dto;

import jakarta.validation.constraints.NotNull;

public record RestProxyRequest(
        @NotNull ApiConfigDTO config
) {}
