package com.react.spring.meta.metaset.dto;

import java.util.List;

public record MetaSetApiSettingDto(
        MetaSetApiAuthDto auth,
        List<MetaSetApiHeaderDto> headers,
        Integer timeoutMs
) {}
