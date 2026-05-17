package com.react.spring.meta.metaset.mapper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.react.spring.meta.metaset.dto.MetaSetApiAuthDto;
import com.react.spring.meta.metaset.dto.MetaSetApiHeaderDto;
import com.react.spring.meta.metaset.dto.MetaSetApiOperationDto;
import com.react.spring.meta.metaset.dto.MetaSetApiSettingDto;
import com.react.spring.meta.metaset.dto.MetaSetEndpointConfigDto;
import com.react.spring.meta.metaset.entity.MetaSetApiSetting;
import com.react.spring.meta.metaset.entity.MetaSetOperation;
import com.react.spring.meta.metaset.entity.dto.MetaSetApiSettingAuthConfig;
import com.react.spring.meta.metaset.entity.dto.MetaSetApiSettingConfig;
import com.react.spring.meta.metaset.entity.dto.MetaSetApiSettingHeaderConfig;
import com.react.spring.meta.metaset.entity.dto.MetaSetOperationConfig;

import java.util.List;

public final class MetaSetApiProjectionMapper {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private MetaSetApiProjectionMapper() {}

    public static MetaSetApiSettingDto toApiSettingDto(MetaSetApiSetting setting) {
        if (setting == null) {
            return null;
        }
        MetaSetApiSettingConfig config = setting.getConfigDto();
        MetaSetApiSettingAuthConfig auth = config != null ? config.getAuth() : null;
        return new MetaSetApiSettingDto(
                new MetaSetApiAuthDto(
                        auth != null ? auth.getAuthType() : "NONE",
                        auth != null ? auth.getUsername() : null,
                        auth != null ? auth.getPassword() : null,
                        auth != null ? auth.getBearerToken() : null,
                        auth != null ? auth.getApiKeyName() : null,
                        auth != null ? auth.getApiKeyValue() : null,
                        auth != null ? auth.getApiKeyPlacement() : null
                ),
                toHeaderDtos(config != null ? config.getHeaders() : List.of()),
                config != null ? config.getTimeoutMs() : null
        );
    }

    public static List<MetaSetApiOperationDto> toOperationDtos(List<MetaSetOperation> operations) {
        if (operations == null) {
            return List.of();
        }
        return operations.stream()
                .map(MetaSetApiProjectionMapper::toOperationDto)
                .toList();
    }

    public static MetaSetEndpointConfigDto parseEndpointConfig(String endpointConfigJson) {
        if (endpointConfigJson == null || endpointConfigJson.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(endpointConfigJson, MetaSetEndpointConfigDto.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<MetaSetApiHeaderDto> toHeaderDtos(List<MetaSetApiSettingHeaderConfig> headers) {
        if (headers == null) {
            return List.of();
        }
        return headers.stream()
                .map(header -> new MetaSetApiHeaderDto(header.getKey(), header.getValue()))
                .toList();
    }

    private static MetaSetApiOperationDto toOperationDto(MetaSetOperation operation) {
        MetaSetOperationConfig config = operation.getConfigDto();
        return new MetaSetApiOperationDto(
                operation.getCode(),
                operation.getName(),
                operation.getOperationType(),
                config != null ? config.getMethod() : null,
                config != null ? config.getEndpoint() : null,
                config != null ? config.getResponseMode() : null,
                config != null ? config.getDescription() : null,
                config != null ? config.getEnabled() : null
        );
    }
}
