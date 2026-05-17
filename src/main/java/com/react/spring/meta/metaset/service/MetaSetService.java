package com.react.spring.meta.metaset.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.react.spring.common.utils.FieldHashUtils;
import com.react.spring.meta.metaset.dto.MetaSetActionRequest;
import com.react.spring.meta.metaset.dto.MetaSetApiAuthDto;
import com.react.spring.meta.metaset.dto.MetaSetApiConfigDto;
import com.react.spring.meta.metaset.dto.MetaSetApiHeaderDto;
import com.react.spring.meta.metaset.dto.MetaSetApiOperationDto;
import com.react.spring.meta.metaset.dto.MetaSetApiSettingDto;
import com.react.spring.meta.metaset.dto.MetaSetDto;
import com.react.spring.meta.metaset.dto.MetaSetEndpointConfigDto;
import com.react.spring.meta.metaset.dto.MetaSetRequest;
import com.react.spring.meta.metaset.entity.MetaSetApiSetting;
import com.react.spring.meta.metaset.entity.MetaSetOperation;
import com.react.spring.meta.metaset.entity.dto.MetaSetApiSettingAuthConfig;
import com.react.spring.meta.metaset.entity.dto.MetaSetApiSettingConfig;
import com.react.spring.meta.metaset.entity.dto.MetaSetApiSettingHeaderConfig;
import com.react.spring.meta.metaset.entity.dto.MetaSetOperationConfig;
import com.react.spring.meta.metasync.dto.FieldItem;
import com.react.spring.meta.metasync.dto.MetaSyncExtractRequest;
import com.react.spring.meta.metasync.entity.MetaSync;
import com.react.spring.meta.metasync.repository.MetaSyncRepository;
import com.react.spring.catalog.entity.Domain;
import com.react.spring.catalog.entity.Organization;
import com.react.spring.meta.metaset.entity.MetaSet;
import com.react.spring.meta.metasetversion.entity.MetaSetVersion;
import com.react.spring.meta.metasource.entity.MetaSource;
import com.react.spring.common.enums.SourceType;
import com.react.spring.common.exception.NotFoundException;
import com.react.spring.meta.metaset.mapper.MetaSetMapper;
import com.react.spring.catalog.repository.DomainRepository;
import com.react.spring.catalog.repository.OrganizationRepository;
import com.react.spring.meta.metaset.repository.MetaSetRepository;
import com.react.spring.meta.metasetversion.repository.MetaSetVersionRepository;
import com.react.spring.meta.metasource.repository.MetaSourceRepository;
import com.react.spring.catalog.repository.TagRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Transactional
public class MetaSetService {
    private static final String TABLE_MARKER_DATA_TYPE = "__TABLE__";
    private static final String STATUS_WARNING = "WARNING";

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_DISCONTINUED = "DISCONTINUED";
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final MetaSetRepository repo;
    private final MetaSetVersionRepository versionRepo;
    private final MetaSourceRepository sourceRepo;
    private final OrganizationRepository orgRepo;
    private final DomainRepository domainRepo;
    private final MetaSyncRepository metaSyncRepo;
    private final TagRepository tagRepo;

    public MetaSetService(MetaSetRepository repo,
                          MetaSetVersionRepository versionRepo,
                          MetaSourceRepository sourceRepo,
                          OrganizationRepository orgRepo,
                          DomainRepository domainRepo,
                          MetaSyncRepository metaSyncRepo,
                          TagRepository tagRepo) {
        this.repo = repo;
        this.versionRepo = versionRepo;
        this.sourceRepo = sourceRepo;
        this.orgRepo = orgRepo;
        this.domainRepo = domainRepo;
        this.metaSyncRepo = metaSyncRepo;
        this.tagRepo = tagRepo;
    }

    @Transactional(readOnly = true)
    public Page<MetaSetDto> list(String keyword, String metaCode, UUID organizationId, UUID domainId, UUID metaSourceId, Pageable pageable) {
        Specification<MetaSet> spec = (root, query, cb) -> cb.conjunction();
        
        if (keyword != null && !keyword.isBlank()) {
            String likeKeyword = "%" + keyword.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("code")), likeKeyword),
                    cb.like(cb.lower(root.get("name")), likeKeyword),
                    cb.like(cb.lower(root.get("metaCode")), likeKeyword)
            ));
        }
        
        if (metaCode != null && !metaCode.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("metaCode")), "%" + metaCode.toLowerCase() + "%"));
        }
        
        if (organizationId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("organization").get("id"), organizationId));
        }
        
        if (domainId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("domain").get("id"), domainId));
        }
        
        if (metaSourceId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("metaSource").get("id"), metaSourceId));
        }
        
        return repo.findAll(spec, pageable).map(MetaSetMapper::toDto);
    }

    @Transactional(readOnly = true)
    public MetaSetDto getById(UUID id) {
        return MetaSetMapper.toDto(loadOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<MetaSetDto> listByMetaSource(UUID metaSourceId) {
        return repo.findAllByMetaSourceId(metaSourceId)
                .stream().map(MetaSetMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<MetaSetDto> listByMetasyncCode(String metasyncCode) {
        if (metasyncCode == null || metasyncCode.isBlank()) {
            return List.of();
        }
        return repo.findAllByMetasyncCode(metasyncCode.trim())
                .stream().map(MetaSetMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public MetaSetDto getByCode(String code) {
        MetaSet e = repo.findByCode(code)
                .orElseThrow(() -> new NotFoundException("MetaSet not found: " + code));
        return MetaSetMapper.toDto(e);
    }

    public MetaSetDto create(MetaSetRequest req) {
        // 1. Resolve refs
        MetaSource source = loadSource(req.metaSourceId());
        Organization org = req.organizationId() != null ? loadOrganization(req.organizationId()) : null;
        Domain dom = req.domainId() != null ? loadDomain(req.domainId()) : null;

        // 2. Generate sequential code (00001, 00002, ...)
        String code = generateNextNumericCode();
        String metaCode = req.metaCode() == null || req.metaCode().isBlank()
                ? (req.name() == null ? null : req.name().trim())
                : req.metaCode().trim();

        // 3. Create initial MetaSetVersion (v1)
        MetaSetVersion v = new MetaSetVersion();
        v.setMetaCode(metaCode);
        v.setDataSourceCode(source != null ? source.getCode() : null);
        v.setVersionNo(1);
        v.setDeleted(Boolean.FALSE);
        v.setChangedStatus("INITIAL");
        v.setChangedSummary("Initial version on creation");
        applyVersionStructure(v, source, req);
        versionRepo.save(v);

        // 4. Create MetaSet linked to v1
        MetaSet ms = new MetaSet();
        ms.setCode(code);
        ms.setMetaCode(metaCode);
        ms.setName(req.name());
        ms.setDescription(req.description());
        ms.setMetaSource(source);
        ms.setOrganization(org);
        ms.setDomain(dom);
        ms.setClassification(req.classification());
        ms.setTier(req.tier());
        ms.setStatus(STATUS_DRAFT);
        ms.setCurrentVersion(v);
        
        if (req.tagIds() != null && !req.tagIds().isEmpty()) {
            ms.setTags(new java.util.HashSet<>(tagRepo.findAllById(req.tagIds())));
        } else {
            ms.setTags(new java.util.HashSet<>());
        }
        
        return MetaSetMapper.toDto(repo.save(ms));
    }

    public MetaSetDto update(UUID id, MetaSetRequest req) {
        MetaSet ms = loadOrThrow(id);
        ms.setName(req.name());
        if (req.metaCode() != null && !req.metaCode().isBlank()) {
            ms.setMetaCode(req.metaCode());
        }
        ms.setDescription(req.description());
        ms.setMetaSource(loadSource(req.metaSourceId()));
        ms.setOrganization(req.organizationId() != null ? loadOrganization(req.organizationId()) : null);
        ms.setDomain(req.domainId() != null ? loadDomain(req.domainId()) : null);
        ms.setClassification(req.classification());
        ms.setTier(req.tier());
        MetaSetVersion currentVersion = ms.getCurrentVersion();
        if (currentVersion == null) {
            currentVersion = new MetaSetVersion();
            currentVersion.setMetaCode(ms.getMetaCode());
            currentVersion.setDataSourceCode(ms.getMetaSource() != null ? ms.getMetaSource().getCode() : null);
            currentVersion.setVersionNo(1);
            currentVersion.setDeleted(Boolean.FALSE);
            currentVersion.setChangedStatus("INITIAL");
            currentVersion.setChangedSummary("Initial version for existing MetaSet");
            ms.setCurrentVersion(currentVersion);
        }
        String previousVersionFingerprint = versionFingerprint(currentVersion);
        MetaSetVersion candidateVersion = new MetaSetVersion();
        copyVersionPayload(currentVersion, candidateVersion);
        candidateVersion.setMetaCode(ms.getMetaCode());
        candidateVersion.setDataSourceCode(ms.getMetaSource() != null ? ms.getMetaSource().getCode() : null);
        applyVersionStructure(candidateVersion, ms.getMetaSource(), req);
        
        if (req.tagIds() != null && !req.tagIds().isEmpty()) {
            ms.setTags(new java.util.HashSet<>(tagRepo.findAllById(req.tagIds())));
        } else {
            if (ms.getTags() != null) {
                ms.getTags().clear();
            } else {
                ms.setTags(new java.util.HashSet<>());
            }
        }
        
        String nextVersionFingerprint = versionFingerprint(candidateVersion);
        if (!Objects.equals(previousVersionFingerprint, nextVersionFingerprint)) {
            if (STATUS_PUBLISHED.equals(ms.getStatus())) {
                MetaSetVersion newV = new MetaSetVersion();
                copyVersionPayload(candidateVersion, newV);
                newV.setMetaCode(ms.getMetaCode());
                newV.setDataSourceCode(ms.getMetaSource() != null ? ms.getMetaSource().getCode() : null);
                newV.setVersionNo(nextVersionNo(currentVersion.getVersionNo()));
                newV.setDeleted(Boolean.FALSE);
                newV.setChangedStatus("MODIFIED");
                newV.setChangedSummary("MetaSet versioned configuration update");
                newV = versionRepo.save(newV);
                ms.setCurrentVersion(newV);
                ms.setStatus(STATUS_DRAFT);
            } else {
                copyVersionPayload(candidateVersion, currentVersion);
                currentVersion.setMetaCode(ms.getMetaCode());
                currentVersion.setDataSourceCode(ms.getMetaSource() != null ? ms.getMetaSource().getCode() : null);
                currentVersion.setChangedStatus("MODIFIED");
                currentVersion.setChangedSummary("MetaSet versioned configuration update");
                versionRepo.save(currentVersion);
            }
        }
        
        return MetaSetMapper.toDto(repo.save(ms));
    }

    public MetaSetDto publish(UUID id, MetaSetActionRequest req) {
        MetaSet ms = loadOrThrow(id);
        if (!STATUS_DRAFT.equals(ms.getStatus())) {
            throw new IllegalStateException(
                    "Only MetaSet in DRAFT status can be published. Current: " + ms.getStatus());
        }
        ms.setStatus(STATUS_PUBLISHED);
        ms.setPublishedAt(OffsetDateTime.now());
        ms.setPublishedBy(req.actor());
        ms.setPublishedComment(req.comment());
        return MetaSetMapper.toDto(repo.save(ms));
    }

    public MetaSetDto discontinue(UUID id, MetaSetActionRequest req) {
        MetaSet ms = loadOrThrow(id);
        if (!STATUS_PUBLISHED.equals(ms.getStatus())) {
            throw new IllegalStateException(
                    "Only PUBLISHED MetaSet can be discontinued. Current: " + ms.getStatus());
        }
        ms.setStatus(STATUS_DISCONTINUED);
        ms.setDiscontinuedAt(OffsetDateTime.now());
        ms.setDiscontinuedBy(req.actor());
        ms.setDiscontinuedComment(req.comment());
        return MetaSetMapper.toDto(repo.save(ms));
    }

    public MetaSetDto extractFromMetaSync(UUID metaSyncId, MetaSyncExtractRequest req) {
        MetaSync sync = metaSyncRepo.findById(metaSyncId)
                .orElseThrow(() -> new NotFoundException("MetaSync not found: " + metaSyncId));

        // Validate: Cannot extract from a deleted MetaSync (deleted=true)
        if (Boolean.TRUE.equals(sync.getDeleted())) {
            throw new IllegalStateException(
                    "Cannot extract from a deleted MetaSync: " + sync.getCode() + " / " + sync.getMetaCode() +
                    ". This table was removed from the source and is marked as deleted.");
        }

        if (req.targetMetaSetId() != null) {
            return appendVersionFromMetaSync(sync, req.targetMetaSetId());
        }
        validateCreateExtractRequest(req);

        Organization org = req.organizationId() != null ? loadOrganization(req.organizationId()) : null;
        Domain dom = req.domainId() != null ? loadDomain(req.domainId()) : null;
        AtomicInteger nextMetaSetCode = new AtomicInteger(repo.findMaxNumericCode() + 1);

        String code = generateNextNumericCode(nextMetaSetCode);

        MetaSetVersion v = new MetaSetVersion();
        v.setMetaCode(sync.getMetaCode());
        v.setDataSourceCode(sync.getMetaSource() != null ? sync.getMetaSource().getCode() : null);
        v.setVersionNo(1);
        v.setMetasyncCode(sync.getCode());
        v.setFieldData(sync.getFieldData());
        v.setFieldHash(sync.getFieldHash());
        v.setDeleted(Boolean.FALSE);
        v.setChangedStatus(sync.getChangedStatus() == null ? STATUS_WARNING : sync.getChangedStatus());
        v.setChangedSummary("Extracted from MetaSync: " + sync.getCode() + " / " + sync.getMetaCode()
                + ". " + detailOrFallback(sync.getChangedSummary(), sync.getFieldData()));
        versionRepo.save(v);

        MetaSet ms = new MetaSet();
        ms.setCode(code);
        ms.setMetaCode(sync.getMetaCode());
        ms.setName(req.name());
        ms.setDescription(req.description());
        ms.setMetaSource(sync.getMetaSource());
        ms.setOrganization(org);
        ms.setDomain(dom);
        ms.setClassification(req.classification());
        ms.setTier(req.tier());
        ms.setStatus(STATUS_DRAFT);
        ms.setCurrentVersion(v);
        return MetaSetMapper.toDto(repo.save(ms));
    }

    private MetaSetDto appendVersionFromMetaSync(MetaSync sync, UUID metaSetId) {
        // Validate: Cannot extract from a deleted MetaSync (deleted=true)
        if (Boolean.TRUE.equals(sync.getDeleted())) {
            throw new IllegalStateException(
                    "Cannot extract from a deleted MetaSync: " + sync.getCode() + " / " + sync.getMetaCode() +
                    ". This table was removed from the source and is marked as deleted.");
        }

        MetaSet ms = loadOrThrow(metaSetId);
        UUID syncSourceId = sync.getMetaSource() == null ? null : sync.getMetaSource().getId();
        UUID metaSetSourceId = ms.getMetaSource() == null ? null : ms.getMetaSource().getId();
        if (syncSourceId == null || !syncSourceId.equals(metaSetSourceId)) {
            throw new IllegalStateException("MetaSync and MetaSet must belong to the same MetaSource");
        }

        Integer nextNo = versionRepo.findByMetaCodeOrderByVersionNoDesc(ms.getMetaCode()).stream()
                .findFirst()
                .map(MetaSetVersion::getVersionNo)
                .orElse(0) + 1;

        MetaSetVersion v = new MetaSetVersion();
        v.setMetaCode(ms.getMetaCode());
        v.setDataSourceCode(sync.getMetaSource().getCode());
        v.setVersionNo(nextNo);
        v.setMetasyncCode(sync.getCode());
        v.setFieldData(sync.getFieldData());
        v.setFieldHash(sync.getFieldHash());
        v.setDeleted(Boolean.FALSE);
        v.setChangedStatus(sync.getChangedStatus() == null ? STATUS_WARNING : sync.getChangedStatus());
        v.setChangedSummary("Updated from MetaSync: " + sync.getCode() + " / " + sync.getMetaCode()
                + ". " + detailOrFallback(sync.getChangedSummary(), sync.getFieldData()));
        versionRepo.save(v);

        ms.setCurrentVersion(v);
        ms.setLastSyncedAt(OffsetDateTime.now());
        ms.setLastSyncStatus(v.getChangedStatus());
        ms.setLastSyncedVersion(nextNo);
        return MetaSetMapper.toDto(repo.save(ms));
    }

    public List<MetaSetDto> extractFromMetaSource(UUID metaSourceId, MetaSyncExtractRequest req) {
        MetaSource source = loadSource(metaSourceId);
        List<MetaSync> syncs = metaSyncRepo.findByMetaSourceIdActiveTrueNotDeletedOrderByMetaCodeAsc(metaSourceId);
        if (syncs.isEmpty()) {
            return List.of();
        }
        AtomicInteger nextMetaSetCode = new AtomicInteger(repo.findMaxNumericCode() + 1);

        if (req.targetMetaSetId() != null) {
            MetaSet target = loadOrThrow(req.targetMetaSetId());
            String targetMetaCode = target.getMetaCode();
            if (targetMetaCode == null || targetMetaCode.isBlank()) {
                throw new IllegalStateException("Target MetaSet must have metaCode to extract from source");
            }
            MetaSync sync = syncs.stream()
                    .filter(item -> targetMetaCode.equals(item.getMetaCode()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "MetaSync not found for table metaCode: " + targetMetaCode));
            return List.of(upsertMetaSetFromSourceSync(source, sync, req, target, null, null, nextMetaSetCode));
        }

        validateCreateExtractRequest(req);
        Organization org = req.organizationId() != null ? loadOrganization(req.organizationId()) : null;
        Domain dom = req.domainId() != null ? loadDomain(req.domainId()) : null;
        List<MetaSetDto> results = new ArrayList<>();
        for (MetaSync sync : syncs) {
            results.add(upsertMetaSetFromSourceSync(source, sync, req, null, org, dom, nextMetaSetCode));
        }
        return results;
    }

    private MetaSetDto upsertMetaSetFromSourceSync(MetaSource source,
                                                   MetaSync sync,
                                                   MetaSyncExtractRequest req,
                                                   MetaSet existingMetaSet,
                                                   Organization org,
                                                   Domain dom,
                                                   AtomicInteger metaSetCodeSequence) {
        String metaCode = sync.getMetaCode();
        if (metaCode == null || metaCode.isBlank()) {
            throw new IllegalStateException("MetaSync metaCode is required");
        }

        MetaSet ms = existingMetaSet;
        if (ms == null) {
            ms = repo.findFirstByMetaSourceIdAndMetaCode(source.getId(), metaCode).orElse(null);
        }

        if (ms == null) {
            ms = new MetaSet();
            ms.setCode(generateNextNumericCode(metaSetCodeSequence));
            ms.setMetaCode(metaCode);
            ms.setMetaSource(source);
            ms.setOrganization(org);
            ms.setDomain(dom);
            ms.setClassification(req.classification());
            ms.setTier(req.tier());
            ms.setStatus(STATUS_DRAFT);
            ms.setDescription(req.description());
            ms.setName(resolveMetaSetName(req.name(), sync));
        } else {
            if (ms.getMetaSource() != null && !source.getId().equals(ms.getMetaSource().getId())) {
                throw new IllegalStateException("MetaSet and MetaSource must belong to the same source");
            }
            if (ms.getMetaSource() == null) {
                ms.setMetaSource(source);
            }
            if (org != null) ms.setOrganization(org);
            if (dom != null) ms.setDomain(dom);
            if (req.description() != null) ms.setDescription(req.description());
            if (req.classification() != null) ms.setClassification(req.classification());
            if (req.tier() != null) ms.setTier(req.tier());
            String resolvedName = resolveMetaSetName(req.name(), sync);
            if (resolvedName != null && !resolvedName.isBlank()) {
                ms.setName(resolvedName);
            }
            ms.setMetaCode(metaCode);
        }

        MetaSetVersion current = ms.getCurrentVersion();
        boolean sameSnapshot = current != null
                && Objects.equals(current.getFieldHash(), sync.getFieldHash())
                && Objects.equals(current.getMetasyncCode(), sync.getCode())
                && Objects.equals(current.getVersionNo(), sync.getVersionNo());

        if (!sameSnapshot) {
            int nextNo = current == null ? 1 : nextVersionNo(current.getVersionNo());
            MetaSetVersion v = new MetaSetVersion();
            v.setMetaCode(metaCode);
            v.setDataSourceCode(source.getCode());
            v.setVersionNo(nextNo);
            v.setMetasyncCode(sync.getCode());
            v.setFieldData(sync.getFieldData());
            v.setFieldHash(sync.getFieldHash());
            v.setDeleted(Boolean.FALSE);
            v.setChangedStatus(sync.getChangedStatus() == null ? STATUS_WARNING : sync.getChangedStatus());
            v.setChangedSummary("Extracted from MetaSource: " + source.getCode() + " / " + metaCode
                    + ". " + detailOrFallback(sync.getChangedSummary(), sync.getFieldData()));
            versionRepo.save(v);

            ms.setCurrentVersion(v);
            ms.setLastSyncedVersion(nextNo);
            ms.setLastSyncStatus(v.getChangedStatus());
        } else {
            ms.setLastSyncedVersion(safeVersionNo(current.getVersionNo()));
            ms.setLastSyncStatus(current.getChangedStatus());
        }

        ms.setLastSyncedAt(OffsetDateTime.now());
        return MetaSetMapper.toDto(repo.save(ms));
    }

    private String summarizeFieldNames(List<FieldItem> fields) {
        if (fields == null || fields.isEmpty()) {
            return "-";
        }
        List<String> names = fields.stream()
                .map(field -> {
                    if (field == null) {
                        return "-";
                    }
                    if (field.path() != null && !field.path().isBlank()) {
                        return field.path();
                    }
                    return field.name() == null || field.name().isBlank() ? "-" : field.name();
                })
                .toList();
        return summarizeStrings(names);
    }

    private String summarizeStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "-";
        }
        int limit = Math.min(values.size(), 12);
        String summary = String.join(", ", values.subList(0, limit));
        int remaining = values.size() - limit;
        return remaining > 0 ? summary + " and " + remaining + " more" : summary;
    }

    private String detailOrFallback(String changedSummary, String fieldData) {
        if (changedSummary != null && !changedSummary.isBlank()) {
            return changedSummary;
        }
        try {
            List<FieldItem> fields = MAPPER.readValue(fieldData, new TypeReference<>() {});
            return "Fields: " + summarizeFieldNames(fields);
        } catch (Exception ignored) {
            return "Fields: -";
        }
    }

    private MetaSetApiConfigDto defaultApiConfig() {
        return new MetaSetApiConfigDto(
                List.of(new MetaSetApiOperationDto(
                        "list",
                        "List",
                        "LIST",
                        "GET",
                        "/",
                        "LIST",
                        null,
                        Boolean.TRUE
                )),
                new MetaSetApiSettingDto(
                        new MetaSetApiAuthDto("NONE", null, null, null, null, null, null),
                        List.of(),
                        30000
                ),
                new MetaSetEndpointConfigDto(null, "list", "LIST")
        );
    }

    private List<MetaSetApiOperationDto> normalizeOperations(List<MetaSetApiOperationDto> operations) {
        if (operations == null || operations.isEmpty()) {
            return defaultApiConfig().operations();
        }
        List<MetaSetApiOperationDto> normalized = operations.stream()
                .filter(Objects::nonNull)
                .map(operation -> new MetaSetApiOperationDto(
                        trimToNull(operation.code()),
                        trimToNull(operation.name()),
                        defaultIfBlank(operation.operationType(), "LIST"),
                        defaultIfBlank(operation.method(), "GET"),
                        defaultIfBlank(operation.endpoint(), "/"),
                        defaultIfBlank(operation.responseMode(), "LIST"),
                        trimToNull(operation.description()),
                        operation.enabled() == null ? Boolean.TRUE : operation.enabled()
                ))
                .toList();
        return normalized.isEmpty() ? defaultApiConfig().operations() : normalized;
    }

    private MetaSetApiSettingDto normalizeApiSetting(MetaSetApiSettingDto apiSetting) {
        MetaSetApiSettingDto current = apiSetting == null
                ? defaultApiConfig().apiSetting()
                : apiSetting;

        MetaSetApiAuthDto auth = current.auth() == null
                ? new MetaSetApiAuthDto("NONE", null, null, null, null, null, null)
                : new MetaSetApiAuthDto(
                        defaultIfBlank(current.auth().authType(), "NONE"),
                        trimToNull(current.auth().username()),
                        trimToNull(current.auth().password()),
                        trimToNull(current.auth().bearerToken()),
                        trimToNull(current.auth().apiKeyName()),
                        trimToNull(current.auth().apiKeyValue()),
                        defaultIfBlank(current.auth().apiKeyPlacement(), "HEADER")
                );

        List<MetaSetApiHeaderDto> headers = current.headers() == null
                ? List.of()
                : current.headers().stream()
                .filter(Objects::nonNull)
                .map(header -> new MetaSetApiHeaderDto(trimToNull(header.key()), trimToNull(header.value())))
                .filter(header -> header.key() != null)
                .toList();

        int timeoutMs = current.timeoutMs() == null || current.timeoutMs() <= 0 ? 30000 : current.timeoutMs();
        return new MetaSetApiSettingDto(auth, headers, timeoutMs);
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void applyVersionStructure(MetaSetVersion version, MetaSource source, MetaSetRequest req) {
        version.setExampleData(trimToNull(req.exampleData()));
        applyFieldData(version, req.fieldData());
        if (source == null || source.getSourceType() != SourceType.API) {
            version.setEndpointPath(trimToNull(req.endpointPath()));
            version.setEndpointConfig(null);
            version.setApiSetting(null);
            version.getOperations().clear();
            return;
        }

        List<MetaSetApiOperationDto> operations = normalizeOperations(req.operations());
        MetaSetApiSettingDto apiSetting = normalizeApiSetting(req.apiSetting());
        MetaSetEndpointConfigDto endpointConfig = normalizeEndpointConfig(req.endpointConfig(), operations);

        version.setEndpointPath(resolvePrimaryEndpoint(endpointConfig, operations));
        version.setEndpointConfig(writeEndpointConfig(endpointConfig));
        version.setApiSetting(toApiSettingEntity(version, apiSetting));
        replaceOperations(version, operations);
    }

    private void applyFieldData(MetaSetVersion version, String fieldData) {
        String normalizedFieldData = trimToNull(fieldData);
        version.setFieldData(normalizedFieldData);
        version.setFieldHash(normalizedFieldData == null ? null : FieldHashUtils.rawJson(normalizedFieldData));
    }

    private MetaSetApiSetting toApiSettingEntity(MetaSetVersion version, MetaSetApiSettingDto dto) {
        MetaSetApiSetting entity = version.getApiSetting() == null ? new MetaSetApiSetting() : version.getApiSetting();
        entity.setMetaSetVersion(version);
        MetaSetApiSettingConfig config = new MetaSetApiSettingConfig();
        MetaSetApiSettingAuthConfig auth = new MetaSetApiSettingAuthConfig();
        auth.setAuthType(defaultIfBlank(dto.auth().authType(), "NONE"));
        auth.setUsername(trimToNull(dto.auth().username()));
        auth.setPassword(trimToNull(dto.auth().password()));
        auth.setBearerToken(trimToNull(dto.auth().bearerToken()));
        auth.setApiKeyName(trimToNull(dto.auth().apiKeyName()));
        auth.setApiKeyValue(trimToNull(dto.auth().apiKeyValue()));
        auth.setApiKeyPlacement(defaultIfBlank(dto.auth().apiKeyPlacement(), "HEADER"));
        config.setAuth(auth);
        config.setHeaders(toHeaderConfigs(dto.headers()));
        config.setTimeoutMs(dto.timeoutMs() == null || dto.timeoutMs() <= 0 ? 30000 : dto.timeoutMs());
        entity.setConfigDto(config);
        return entity;
    }

    private void replaceOperations(MetaSetVersion version, List<MetaSetApiOperationDto> operations) {
        version.getOperations().clear();
        for (int index = 0; index < operations.size(); index++) {
            MetaSetApiOperationDto dto = operations.get(index);
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

    private void copyVersionPayload(MetaSetVersion source, MetaSetVersion target) {
        target.setFieldData(source.getFieldData());
        target.setFieldHash(source.getFieldHash());
        target.setExampleData(source.getExampleData());
        target.setEndpointPath(source.getEndpointPath());
        target.setEndpointConfig(source.getEndpointConfig());
        if (source.getApiSetting() != null) {
            MetaSetApiSetting copiedSetting = new MetaSetApiSetting();
            copiedSetting.setConfig(source.getApiSetting().getConfig());
            copiedSetting.setMetaSetVersion(target);
            target.setApiSetting(copiedSetting);
        } else {
            target.setApiSetting(null);
        }
        target.getOperations().clear();
        if (source.getOperations() != null) {
            for (MetaSetOperation sourceOperation : source.getOperations()) {
                MetaSetOperation copiedOperation = new MetaSetOperation();
                copiedOperation.setMetaSetVersion(target);
                copiedOperation.setCode(sourceOperation.getCode());
                copiedOperation.setName(sourceOperation.getName());
                copiedOperation.setOperationType(sourceOperation.getOperationType());
                copiedOperation.setConfig(sourceOperation.getConfig());
                copiedOperation.setSortOrder(sourceOperation.getSortOrder());
                target.getOperations().add(copiedOperation);
            }
        }
    }

    private String versionFingerprint(MetaSetVersion version) {
        try {
            return MAPPER.writeValueAsString(new VersionFingerprint(
                    trimToNull(version.getFieldData()),
                    trimToNull(version.getFieldHash()),
                    trimToNull(version.getExampleData()),
                    trimToNull(version.getEndpointPath()),
                    trimToNull(version.getEndpointConfig()),
                    version.getApiSetting() != null ? trimToNull(version.getApiSetting().getConfig()) : null,
                    version.getOperations() == null
                            ? List.of()
                            : version.getOperations().stream()
                            .map(operation -> new OperationFingerprint(
                                    trimToNull(operation.getCode()),
                                    trimToNull(operation.getName()),
                                    trimToNull(operation.getOperationType()),
                                    trimToNull(operation.getConfig()),
                                    operation.getSortOrder()
                            ))
                            .toList()
            ));
        } catch (Exception e) {
            throw new IllegalStateException("Khong the tinh fingerprint cua MetaSetVersion", e);
        }
    }

    private List<MetaSetApiSettingHeaderConfig> toHeaderConfigs(List<MetaSetApiHeaderDto> headers) {
        if (headers == null) {
            return List.of();
        }
        return headers.stream()
                .filter(Objects::nonNull)
                .map(header -> new MetaSetApiSettingHeaderConfig(trimToNull(header.key()), trimToNull(header.value())))
                .filter(header -> header.getKey() != null)
                .toList();
    }

    private MetaSetEndpointConfigDto normalizeEndpointConfig(MetaSetEndpointConfigDto endpointConfig,
                                                             List<MetaSetApiOperationDto> operations) {
        MetaSetApiOperationDto primaryOperation = selectPrimaryOperation(
                endpointConfig != null ? trimToNull(endpointConfig.primaryOperationCode()) : null,
                endpointConfig != null ? trimToNull(endpointConfig.primaryOperationType()) : null,
                operations
        );
        String primaryCode = primaryOperation != null ? trimToNull(primaryOperation.code()) : null;
        String primaryType = primaryOperation != null ? defaultIfBlank(primaryOperation.operationType(), "CUSTOM") : null;
        return new MetaSetEndpointConfigDto(
                endpointConfig != null ? trimToNull(endpointConfig.basePath()) : null,
                primaryCode,
                primaryType
        );
    }

    private String resolvePrimaryEndpoint(MetaSetEndpointConfigDto endpointConfig, List<MetaSetApiOperationDto> operations) {
        MetaSetApiOperationDto primaryOperation = selectPrimaryOperation(
                endpointConfig != null ? trimToNull(endpointConfig.primaryOperationCode()) : null,
                endpointConfig != null ? trimToNull(endpointConfig.primaryOperationType()) : null,
                operations
        );
        String endpoint = primaryOperation != null ? trimToNull(primaryOperation.endpoint()) : null;
        return endpoint != null ? endpoint : deriveEndpointPath(operations);
    }

    private String deriveEndpointPath(List<MetaSetApiOperationDto> operations) {
        if (operations == null || operations.isEmpty()) {
            return null;
        }
        List<String> priority = List.of("LIST", "DETAIL", "CREATE", "UPDATE", "DELETE", "CUSTOM");
        List<MetaSetApiOperationDto> enabledOperations = operations.stream()
                .filter(Objects::nonNull)
                .filter(operation -> !Boolean.FALSE.equals(operation.enabled()))
                .toList();
        for (String operationType : priority) {
            for (MetaSetApiOperationDto operation : enabledOperations) {
                if (operationType.equalsIgnoreCase(defaultIfBlank(operation.operationType(), "CUSTOM"))) {
                    String path = trimToNull(operation.endpoint());
                    if (path != null) {
                        return path;
                    }
                }
            }
        }
        return enabledOperations.stream()
                .map(MetaSetApiOperationDto::endpoint)
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private MetaSetApiOperationDto selectPrimaryOperation(String preferredCode,
                                                          String preferredType,
                                                          List<MetaSetApiOperationDto> operations) {
        if (operations == null || operations.isEmpty()) {
            return null;
        }
        List<MetaSetApiOperationDto> enabledOperations = operations.stream()
                .filter(Objects::nonNull)
                .filter(operation -> !Boolean.FALSE.equals(operation.enabled()))
                .toList();
        if (preferredCode != null) {
            MetaSetApiOperationDto byCode = enabledOperations.stream()
                    .filter(operation -> preferredCode.equalsIgnoreCase(defaultIfBlank(operation.code(), "")))
                    .findFirst()
                    .orElse(null);
            if (byCode != null) {
                return byCode;
            }
        }
        if (preferredType != null) {
            MetaSetApiOperationDto byType = enabledOperations.stream()
                    .filter(operation -> preferredType.equalsIgnoreCase(defaultIfBlank(operation.operationType(), "CUSTOM")))
                    .findFirst()
                    .orElse(null);
            if (byType != null) {
                return byType;
            }
        }
        String derivedPath = deriveEndpointPath(enabledOperations);
        if (derivedPath != null) {
            return enabledOperations.stream()
                    .filter(operation -> derivedPath.equals(trimToNull(operation.endpoint())))
                    .findFirst()
                    .orElse(enabledOperations.get(0));
        }
        return enabledOperations.isEmpty() ? null : enabledOperations.get(0);
    }

    private String writeEndpointConfig(MetaSetEndpointConfigDto endpointConfig) {
        try {
            return MAPPER.writeValueAsString(endpointConfig);
        } catch (Exception e) {
            throw new IllegalArgumentException("Khong the chuan hoa endpointConfig cua MetaSet", e);
        }
    }

    public void delete(UUID id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("MetaSet not found: " + id);
        }
        repo.deleteById(id);
    }

    private String generateNextNumericCode() {
        return generateNextNumericCode(new AtomicInteger(repo.findMaxNumericCode() + 1));
    }

    private String generateNextNumericCode(AtomicInteger sequence) {
        if (sequence == null) {
            return generateNextNumericCode();
        }
        String candidate;
        do {
            candidate = String.format("%05d", sequence.getAndIncrement());
        } while (repo.existsByCode(candidate));
        return candidate;
    }

    private int safeVersionNo(Integer versionNo) {
        return versionNo == null ? 1 : versionNo;
    }

    private int nextVersionNo(Integer versionNo) {
        return safeVersionNo(versionNo) + 1;
    }

    private String resolveMetaSetName(String requestedName, MetaSync sync) {
        if (requestedName != null && !requestedName.isBlank()) {
            return requestedName.trim();
        }
        if (sync != null && sync.getMetaName() != null && !sync.getMetaName().isBlank()) {
            return sync.getMetaName();
        }
        if (sync != null && sync.getMetaCode() != null && !sync.getMetaCode().isBlank()) {
            return sync.getMetaCode();
        }
        return requestedName;
    }

    private void validateCreateExtractRequest(MetaSyncExtractRequest req) {
        if (req.name() == null || req.name().isBlank()) {
            throw new IllegalArgumentException("MetaSet name is required");
        }
    }

    private MetaSet loadOrThrow(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("MetaSet not found: " + id));
    }

    private MetaSource loadSource(UUID id) {
        return sourceRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("MetaSource not found: " + id));
    }

    private Organization loadOrganization(UUID id) {
        return orgRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Organization not found: " + id));
    }

    private Domain loadDomain(UUID id) {
        return domainRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Domain not found: " + id));
    }

    private record VersionFingerprint(
            String fieldData,
            String fieldHash,
            String exampleData,
            String endpointPath,
            String endpointConfig,
            String apiSettingConfig,
            List<OperationFingerprint> operations
    ) {}

    private record OperationFingerprint(
            String code,
            String name,
            String operationType,
            String config,
            Integer sortOrder
    ) {}
}
