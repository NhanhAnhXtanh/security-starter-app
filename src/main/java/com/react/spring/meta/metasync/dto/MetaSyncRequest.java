package com.react.spring.meta.metasync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record MetaSyncRequest(
        @Size(max = 50) String status,
        UUID dataSourceId,
        @Size(max = 200) String metaCode,
        @Size(max = 500) String metaName,
        @NotNull String fieldData,
        @NotBlank String fieldHash,
        Boolean deleted,
        Boolean isActive,
        @NotNull Integer versionNo,
        @NotBlank String changedStatus,
        @NotBlank String changedSummary
) {}
