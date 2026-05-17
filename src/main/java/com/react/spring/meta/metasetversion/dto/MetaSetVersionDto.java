package com.react.spring.meta.metasetversion.dto;

import com.react.spring.meta.metaset.dto.MetaSetApiOperationDto;
import com.react.spring.meta.metaset.dto.MetaSetApiSettingDto;
import com.react.spring.meta.metaset.dto.MetaSetEndpointConfigDto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record MetaSetVersionDto(
        UUID id,
        String dataSourceCode,
        String metaCode,
        Integer versionNo,
        String metasyncCode,
        String fieldData,
        String fieldHash,
        String exampleData,
        String endpointPath,
        MetaSetEndpointConfigDto endpointConfig,
        MetaSetApiSettingDto apiSetting,
        List<MetaSetApiOperationDto> operations,
        Boolean deleted,
        String changedStatus,
        String changedSummary,
        OffsetDateTime createdDate,
        OffsetDateTime lastModifiedDate
) {}
