package com.react.spring.meta.metaset.dto;

import java.util.List;

public record MetaSetApiConfigDto(
        List<MetaSetApiOperationDto> operations,
        MetaSetApiSettingDto apiSetting,
        MetaSetEndpointConfigDto endpointConfig
) {}
