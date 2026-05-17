package com.react.spring.catalog.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrganizationDto(
        UUID id,
        String name,
        String description,
        OffsetDateTime createdDate,
        OffsetDateTime lastModifiedDate
) {}
