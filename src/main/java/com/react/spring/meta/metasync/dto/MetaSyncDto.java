package com.react.spring.meta.metasync.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MetaSyncDto(
        UUID id,
        String code,
        String status,
        UUID dataSourceId,
        String dataSourceCode,
        String metaCode,
        String metaName,
        String fieldData,
        String fieldHash,
        Boolean deleted,
        Boolean isActive,
        Integer versionNo,
        String changedStatus,
        String changedSummary,
        OffsetDateTime createdDate,
        OffsetDateTime lastModifiedDate
) {}
