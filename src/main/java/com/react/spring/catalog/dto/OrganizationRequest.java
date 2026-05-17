package com.react.spring.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrganizationRequest(
        @NotBlank @Size(max = 500) String name,
        @Size(max = 2000) String description
) {}
