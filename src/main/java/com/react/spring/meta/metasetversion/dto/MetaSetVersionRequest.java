package com.react.spring.meta.metasetversion.dto;

import com.react.spring.meta.metaset.dto.MetaSetApiOperationDto;
import com.react.spring.meta.metaset.dto.MetaSetApiSettingDto;
import com.react.spring.meta.metaset.dto.MetaSetEndpointConfigDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MetaSetVersionRequest(
        @Size(max = 200) String dataSourceCode,
        @NotBlank @Size(max = 200) String metaCode,
        @Size(max = 200) String metasyncCode,
        String fieldData,
        @Size(max = 128) String fieldHash,
        String exampleData,
        @Size(max = 500) String endpointPath,
        MetaSetEndpointConfigDto endpointConfig,
        MetaSetApiSettingDto apiSetting,
        List<MetaSetApiOperationDto> operations,
        Boolean deleted,
        @NotBlank String changedStatus,
        String changedSummary
) {}
