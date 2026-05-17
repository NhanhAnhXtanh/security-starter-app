package com.react.spring.meta.metasetversion.mapper;

import com.react.spring.meta.metaset.dto.MetaSetApiOperationDto;
import com.react.spring.meta.metaset.dto.MetaSetApiSettingDto;
import com.react.spring.meta.metaset.dto.MetaSetEndpointConfigDto;
import com.react.spring.meta.metaset.mapper.MetaSetApiProjectionMapper;
import com.react.spring.meta.metasetversion.dto.MetaSetVersionDto;
import com.react.spring.meta.metasetversion.entity.MetaSetVersion;

import java.util.List;

public final class MetaSetVersionMapper {
    private MetaSetVersionMapper() {}

    public static MetaSetVersionDto toDto(MetaSetVersion e) {
        MetaSetEndpointConfigDto endpointConfig = MetaSetApiProjectionMapper.parseEndpointConfig(e.getEndpointConfig());
        MetaSetApiSettingDto apiSetting = MetaSetApiProjectionMapper.toApiSettingDto(e.getApiSetting());
        List<MetaSetApiOperationDto> operations = MetaSetApiProjectionMapper.toOperationDtos(e.getOperations());
        return new MetaSetVersionDto(
                e.getId(),
                e.getDataSourceCode(),
                e.getMetaCode(),
                e.getVersionNo(),
                e.getMetasyncCode(),
                e.getFieldData(),
                e.getFieldHash(),
                e.getExampleData(),
                e.getEndpointPath(),
                endpointConfig,
                apiSetting,
                operations,
                e.getDeleted(),
                e.getChangedStatus(),
                e.getChangedSummary(),
                e.getCreatedDate(),
                e.getLastModifiedDate()
        );
    }
}
