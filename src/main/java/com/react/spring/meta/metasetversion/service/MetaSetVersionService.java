package com.react.spring.meta.metasetversion.service;

import com.react.spring.meta.metasetversion.dto.MetaSetVersionDto;
import com.react.spring.meta.metasetversion.dto.MetaSetVersionRequest;
import com.react.spring.meta.metaset.entity.MetaSetApiSetting;
import com.react.spring.meta.metaset.entity.MetaSetOperation;
import com.react.spring.meta.metaset.entity.dto.MetaSetApiSettingAuthConfig;
import com.react.spring.meta.metaset.entity.dto.MetaSetApiSettingConfig;
import com.react.spring.meta.metaset.entity.dto.MetaSetApiSettingHeaderConfig;
import com.react.spring.meta.metaset.entity.dto.MetaSetOperationConfig;
import com.react.spring.meta.metasetversion.entity.MetaSetVersion;
import com.react.spring.common.exception.NotFoundException;
import com.react.spring.meta.metasetversion.mapper.MetaSetVersionMapper;
import com.react.spring.meta.metasetversion.repository.MetaSetVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class MetaSetVersionService {

    private final MetaSetVersionRepository repo;

    public MetaSetVersionService(MetaSetVersionRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<MetaSetVersionDto> listByMetaCode(String metaCode) {
        return repo.findByMetaCodeOrderByVersionNoDesc(metaCode).stream()
                .map(MetaSetVersionMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public MetaSetVersionDto getById(UUID id) {
        return MetaSetVersionMapper.toDto(loadOrThrow(id));
    }

    @Transactional(readOnly = true)
    public MetaSetVersionDto getByMetaCodeAndVersionNo(String metaCode, Integer versionNo) {
        MetaSetVersion e = repo.findByMetaCodeAndVersionNo(metaCode, versionNo)
                .orElseThrow(() -> new NotFoundException(
                        "MetaSetVersion not found: " + metaCode + " v" + versionNo));
        return MetaSetVersionMapper.toDto(e);
    }

    public MetaSetVersionDto create(MetaSetVersionRequest req) {
        Integer nextNo = repo.findByMetaCodeOrderByVersionNoDesc(req.metaCode()).stream()
                .findFirst()
                .map(MetaSetVersion::getVersionNo)
                .orElse(0) + 1;
        MetaSetVersion e = new MetaSetVersion();
        e.setMetaCode(req.metaCode());
        e.setDataSourceCode(req.dataSourceCode());
        e.setVersionNo(nextNo);
        e.setMetasyncCode(req.metasyncCode());
        e.setFieldData(req.fieldData());
        e.setFieldHash(req.fieldHash());
        e.setExampleData(req.exampleData());
        e.setEndpointPath(req.endpointPath());
        e.setEndpointConfig(writeEndpointConfig(req.endpointConfig()));
        e.setApiSetting(toApiSettingEntity(req));
        replaceOperations(e, req);
        e.setDeleted(req.deleted() == null ? Boolean.FALSE : req.deleted());
        e.setChangedStatus(req.changedStatus());
        e.setChangedSummary(req.changedSummary());
        return MetaSetVersionMapper.toDto(repo.save(e));
    }

    public void delete(UUID id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("MetaSetVersion not found: " + id);
        }
        repo.deleteById(id);
    }

    private MetaSetVersion loadOrThrow(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("MetaSetVersion not found: " + id));
    }

    private MetaSetApiSetting toApiSettingEntity(MetaSetVersionRequest req) {
        if (req.apiSetting() == null) {
            return null;
        }
        MetaSetApiSetting entity = new MetaSetApiSetting();
        MetaSetApiSettingConfig config = new MetaSetApiSettingConfig();
        MetaSetApiSettingAuthConfig auth = new MetaSetApiSettingAuthConfig();
        auth.setAuthType(defaultIfBlank(req.apiSetting().auth().authType(), "NONE"));
        auth.setUsername(trimToNull(req.apiSetting().auth().username()));
        auth.setPassword(trimToNull(req.apiSetting().auth().password()));
        auth.setBearerToken(trimToNull(req.apiSetting().auth().bearerToken()));
        auth.setApiKeyName(trimToNull(req.apiSetting().auth().apiKeyName()));
        auth.setApiKeyValue(trimToNull(req.apiSetting().auth().apiKeyValue()));
        auth.setApiKeyPlacement(defaultIfBlank(req.apiSetting().auth().apiKeyPlacement(), "HEADER"));
        config.setAuth(auth);
        config.setHeaders(
                req.apiSetting().headers() == null
                        ? List.of()
                        : req.apiSetting().headers().stream()
                        .filter(Objects::nonNull)
                        .map(header -> new MetaSetApiSettingHeaderConfig(trimToNull(header.key()), trimToNull(header.value())))
                        .filter(header -> header.getKey() != null)
                        .toList()
        );
        config.setTimeoutMs(req.apiSetting().timeoutMs() == null || req.apiSetting().timeoutMs() <= 0 ? 30000 : req.apiSetting().timeoutMs());
        entity.setConfigDto(config);
        return entity;
    }

    private void replaceOperations(MetaSetVersion version, MetaSetVersionRequest req) {
        version.getOperations().clear();
        if (req.operations() == null) {
            return;
        }
        for (int index = 0; index < req.operations().size(); index++) {
            var dto = req.operations().get(index);
            if (dto == null) {
                continue;
            }
            MetaSetOperation entity = new MetaSetOperation();
            entity.setMetaSetVersion(version);
            entity.setCode(trimToNull(dto.code()));
            entity.setName(trimToNull(dto.name()));
            entity.setOperationType(defaultIfBlank(dto.operationType(), "LIST"));
            MetaSetOperationConfig config = new MetaSetOperationConfig();
            config.setMethod(defaultIfBlank(dto.method(), "GET"));
            config.setEndpoint(defaultIfBlank(dto.endpoint(), "/"));
            config.setResponseMode(defaultIfBlank(dto.responseMode(), "LIST"));
            config.setDescription(trimToNull(dto.description()));
            config.setEnabled(dto.enabled() == null ? Boolean.TRUE : dto.enabled());
            entity.setConfigDto(config);
            entity.setSortOrder(index);
            version.getOperations().add(entity);
        }
    }

    private String writeEndpointConfig(Object endpointConfig) {
        if (endpointConfig == null) {
            return null;
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(endpointConfig);
        } catch (Exception e) {
            throw new IllegalArgumentException("Khong the ghi endpointConfig cua MetaSetVersion", e);
        }
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

}
