package com.react.spring.catalog.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TagDto(
        UUID id,
        String name,
        String description,
        OffsetDateTime createdDate,
        OffsetDateTime lastModifiedDate
) {}
