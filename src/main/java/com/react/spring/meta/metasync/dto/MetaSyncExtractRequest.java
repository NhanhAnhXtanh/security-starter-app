package com.react.spring.meta.metasync.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public record MetaSyncExtractRequest(
        UUID targetMetaSetId,
        @Size(max = 500) String name,
        String description,
        UUID organizationId,
        UUID domainId,
        @Size(max = 50) String classification,
        @Size(max = 50) String tier
) {}
