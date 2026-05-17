package com.react.spring.meta.metaset.mapper;

import com.react.spring.meta.metaset.dto.MetaSetApiOperationDto;
import com.react.spring.meta.metaset.dto.MetaSetApiSettingDto;
import com.react.spring.meta.metaset.dto.MetaSetDto;
import com.react.spring.meta.metaset.dto.MetaSetEndpointConfigDto;
import com.react.spring.catalog.entity.Domain;
import com.react.spring.catalog.entity.Organization;
import com.react.spring.meta.metaset.entity.MetaSet;
import com.react.spring.meta.metasetversion.entity.MetaSetVersion;
import com.react.spring.meta.metasource.entity.MetaSource;
import com.react.spring.catalog.dto.TagDto;

import java.util.List;
import java.util.stream.Collectors;

public final class MetaSetMapper {
    private MetaSetMapper() {}

    public static MetaSetDto toDto(MetaSet e) {
        MetaSource src = e.getMetaSource();
        Organization org = e.getOrganization();
        Domain dom = e.getDomain();
        MetaSetVersion ver = e.getCurrentVersion();
        List<MetaSetApiOperationDto> operationDtos = MetaSetApiProjectionMapper.toOperationDtos(ver != null ? ver.getOperations() : null);
        MetaSetEndpointConfigDto endpointConfig = MetaSetApiProjectionMapper.parseEndpointConfig(ver != null ? ver.getEndpointConfig() : null);
        if (endpointConfig == null && !operationDtos.isEmpty()) {
            MetaSetApiOperationDto primaryOperation = operationDtos.stream()
                    .filter(operation -> !Boolean.FALSE.equals(operation.enabled()))
                    .findFirst()
                    .orElse(operationDtos.get(0));
            endpointConfig = new MetaSetEndpointConfigDto(
                    null,
                    primaryOperation.code(),
                    primaryOperation.operationType()
            );
        }
        MetaSetApiSettingDto apiSettingDto = MetaSetApiProjectionMapper.toApiSettingDto(ver != null ? ver.getApiSetting() : null);
        return new MetaSetDto(
                e.getId(),
                e.getCode(),
                e.getMetaCode(),
                e.getName(),
                e.getDescription(),
                src != null ? src.getId() : null,
                src != null ? src.getCode() : null,
                src != null ? src.getName() : null,
                org != null ? org.getId() : null,
                org != null ? org.getName() : null,
                dom != null ? dom.getId() : null,
                dom != null ? dom.getName() : null,
                e.getClassification(),
                e.getTier(),
                e.getStatus(),
                e.getPublishedAt(),
                e.getPublishedBy(),
                e.getPublishedComment(),
                e.getDiscontinuedAt(),
                e.getDiscontinuedBy(),
                e.getDiscontinuedComment(),
                e.getLastSyncedAt(),
                e.getLastSyncStatus(),
                e.getLastSyncedVersion(),
                ver != null ? ver.getExampleData() : null,
                ver != null ? ver.getEndpointPath() : null,
                endpointConfig,
                apiSettingDto,
                operationDtos,
                ver != null ? ver.getId() : null,
                ver != null ? ver.getVersionNo() : null,
                e.getCreatedDate(),
                e.getLastModifiedDate(),
                e.getTags() != null ? e.getTags().stream().map(tag -> new TagDto(tag.getId(), tag.getName(), tag.getDescription(), tag.getCreatedDate(), tag.getLastModifiedDate())).collect(Collectors.toList()) : null
        );
    }
}
