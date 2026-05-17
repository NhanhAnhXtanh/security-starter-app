package com.react.spring.meta.metasync.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.react.spring.meta.metasync.dto.FieldItem;
import com.react.spring.meta.metasync.dto.MetaSyncDto;
import com.react.spring.meta.metasync.dto.MetaSyncRequest;
import com.react.spring.meta.metasource.connect.db.dto.SchemaDto;
import com.react.spring.meta.metasource.connect.db.dto.SyncResultDto;
import com.react.spring.meta.metasource.entity.MetaSource;
import com.react.spring.meta.metasync.entity.MetaSync;
import com.react.spring.common.exception.NotFoundException;
import com.react.spring.meta.metasync.mapper.MetaSyncMapper;
import com.react.spring.meta.metaset.repository.MetaSetRepository;
import com.react.spring.meta.metasource.repository.MetaSourceRepository;
import com.react.spring.meta.metasync.repository.MetaSyncRepository;
import com.react.spring.meta.metasetversion.repository.MetaSetVersionRepository;
import com.react.spring.meta.metasource.connect.db.MetaSourceConnectionService;
import com.react.spring.common.utils.FieldHashUtils;
import com.react.spring.common.utils.SlugUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class MetaSyncService {

    public static final String TABLE_MARKER_DATA_TYPE = "__TABLE__";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_OFFLINE = "OFFLINE";
    private static final String SYNC_MODE_ONLINE = "ONLINE";
    private static final String SYNC_MODE_OFFLINE = "OFFLINE";

    private final MetaSyncRepository repo;
    private final MetaSourceRepository sourceRepo;
    private final MetaSetRepository metaSetRepo;
    private final MetaSetVersionRepository metaSetVersionRepo;
    private final MetaSourceConnectionService connectionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MetaSyncService(MetaSyncRepository repo,
                           MetaSourceRepository sourceRepo,
                           MetaSetRepository metaSetRepo,
                           MetaSetVersionRepository metaSetVersionRepo,
                           MetaSourceConnectionService connectionService) {
        this.repo = repo;
        this.sourceRepo = sourceRepo;
        this.metaSetRepo = metaSetRepo;
        this.metaSetVersionRepo = metaSetVersionRepo;
        this.connectionService = connectionService;
    }

    // ...existing code...
    @Transactional(readOnly = true)
    public Page<MetaSyncDto> list(UUID dataSourceId, UUID organizationId, UUID domainId, String keyword, Pageable pageable) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Page<MetaSync> page = kw == null
                ? repo.findWithFilters(dataSourceId, organizationId, domainId, pageable)
                : repo.findWithFiltersAndKeyword(dataSourceId, organizationId, domainId, kw, pageable);
        return page
                .map(MetaSyncMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<MetaSyncDto> listBySource(UUID metaSourceId) {
        return repo.findByMetaSourceIdAndActiveTrueOrderByMetaCodeAsc(metaSourceId).stream()
                .map(MetaSyncMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public MetaSyncDto getById(UUID id) {
        return MetaSyncMapper.toDto(loadOrThrow(id));
    }

    @Transactional(readOnly = true)
    public MetaSyncDto getByCode(String code) {
        MetaSync e = repo.findFirstByCodeAndActiveTrueOrderByMetaCodeAscVersionNoDesc(code)
                .orElseThrow(() -> new NotFoundException("MetaSync not found: " + code));
        return MetaSyncMapper.toDto(e);
    }

    public MetaSyncDto create(MetaSyncRequest req) {
        MetaSync e = new MetaSync();
        applyRequest(e, req);
        if (e.getMetaSource() != null) {
            e.setCode(resolveMetaSyncCode(e.getMetaSource()));
        } else {
            e.setCode(generateNextNumericCode());
        }
        deactivatePeersWhenActive(e, null);
        return MetaSyncMapper.toDto(repo.save(e));
    }

    public MetaSyncDto update(UUID id, MetaSyncRequest req) {
        MetaSync e = loadOrThrow(id);
        applyRequest(e, req);
        deactivatePeersWhenActive(e, id);
        return MetaSyncMapper.toDto(repo.save(e));
    }

    public void delete(UUID id) {
        MetaSync sync = loadOrThrow(id);
        String metaSyncCode = sync.getCode();

        // Cascade delete: remove all MetaSets and MetaSetVersions that were extracted from this MetaSync
        if (metaSyncCode != null && !metaSyncCode.isBlank()) {
            // Delete all MetaSetVersions that reference this MetaSync
            metaSetVersionRepo.deleteByMetasyncCode(metaSyncCode);

            // Delete all MetaSets that were extracted from this MetaSync
            metaSetRepo.deleteAllByMetasyncCode(metaSyncCode);
        }

        // Finally delete the MetaSync itself
        repo.deleteById(id);
    }

    // ...existing code...
    public SyncResultDto initSync(UUID metaSourceId) {
        MetaSource source = loadSource(metaSourceId);
        SchemaDto schema;
        try {
            schema = connectionService.fetchSchema(metaSourceId);
        } catch (RuntimeException ex) {
            return useOfflineMetaSync(metaSourceId, ex);
        }

        List<MetaSyncDto> changedItems = new ArrayList<>();
        int skipped = 0;
        Map<String, SchemaDto.TableDto> currentTables = schema.tables().stream()
                .collect(Collectors.toMap(SchemaDto.TableDto::name, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        String syncCode = resolveMetaSyncCode(source);

        for (SchemaDto.TableDto table : schema.tables()) {
            List<FieldItem> fields = toFieldItems(table);
            String fieldDataJson = serializeFields(fields);
            String structuralHash = FieldHashUtils.structural(fields);

            // Dùng OrderByVersionNoDesc để tìm version mới nhất bất kể active=null/false/true
            MetaSync latest = repo.findFirstByMetaSourceIdAndMetaCodeOrderByVersionNoDesc(metaSourceId, table.name())
                    .orElse(null);

            if (latest != null
                    && !Boolean.TRUE.equals(latest.getDeleted())
                    && Objects.equals(latest.getFieldHash(), structuralHash)) {
                // Đảm bảo version hiện tại luôn được đánh dấu active (fix null records cũ)
                if (!Boolean.TRUE.equals(latest.getActive()) || STATUS_OFFLINE.equals(latest.getStatus())) {
                    latest.setActive(Boolean.TRUE);
                    latest.setStatus(STATUS_ACTIVE);
                    repo.save(latest);
                }
                skipped++;
                continue;
            }

            int versionNo = latest == null ? 1 : nextVersionNo(latest.getVersionNo());
            String changedStatus = latest == null
                    ? "WARNING"
                    : Boolean.TRUE.equals(latest.getDeleted()) ? "WARNING"
                    : computeChangedStatus(latest.getFieldData(), fields);

            // deactivateAll xử lý cả null records (khác deactivateActive chỉ match active=true)
            repo.deactivateAll(metaSourceId, table.name());

            MetaSync e = new MetaSync();
            e.setCode(syncCode);
            e.setStatus(STATUS_ACTIVE);
            e.setMetaSource(source);
            e.setMetaCode(table.name());
            e.setMetaName(table.name());
            e.setFieldData(fieldDataJson);
            e.setFieldHash(structuralHash);
            e.setDeleted(Boolean.FALSE);
            e.setActive(Boolean.TRUE);
            e.setVersionNo(versionNo);
            e.setChangedStatus(changedStatus);
            e.setChangedSummary(buildChangedSummary(changedStatus, source.getName(), table.name(), fields.size()));

            changedItems.add(MetaSyncMapper.toDto(repo.save(e)));
        }

        // Detect bảng bị xóa: dùng findLatestPerMetaCode để lấy version mới nhất mỗi bảng
        for (MetaSync latest : repo.findLatestPerMetaCode(metaSourceId)) {
            String metaCode = latest.getMetaCode();
            if (metaCode == null || currentTables.containsKey(metaCode) || Boolean.TRUE.equals(latest.getDeleted())) {
                continue;
            }

            repo.deactivateAll(metaSourceId, metaCode);

            MetaSync deleted = new MetaSync();
            int versionNo = nextVersionNo(latest.getVersionNo());
            deleted.setCode(syncCode);
            deleted.setStatus(STATUS_ACTIVE);
            deleted.setMetaSource(source);
            deleted.setMetaCode(metaCode);
            deleted.setMetaName(latest.getMetaName());
            deleted.setFieldData(latest.getFieldData());
            deleted.setFieldHash(latest.getFieldHash());
            deleted.setDeleted(Boolean.TRUE);
            deleted.setActive(Boolean.TRUE);
            deleted.setVersionNo(versionNo);
            deleted.setChangedStatus("CRITICAL");
            deleted.setChangedSummary("Removed " + metaCode + " from " + source.getName());
            changedItems.add(MetaSyncMapper.toDto(repo.save(deleted)));
        }

        return new SyncResultDto(changedItems.size(), skipped, changedItems, SYNC_MODE_ONLINE, null);
    }

    private SyncResultDto useOfflineMetaSync(UUID metaSourceId, RuntimeException cause) {
        List<MetaSync> latestItems = repo.findByMetaSourceIdAndActiveTrueOrderByMetaCodeAsc(metaSourceId);
        List<MetaSyncDto> items = latestItems.stream()
                .map(item -> {
                    if (!STATUS_OFFLINE.equals(item.getStatus())) {
                        item.setStatus(STATUS_OFFLINE);
                        repo.save(item);
                    }
                    return MetaSyncMapper.toDto(item);
                })
                .toList();
        String message = "Cannot connect to database; using cached MetaSync in offline mode"
                + (cause.getMessage() == null || cause.getMessage().isBlank() ? "" : ": " + cause.getMessage());
        return new SyncResultDto(0, items.size(), items, SYNC_MODE_OFFLINE, message);
    }

    private List<FieldItem> toFieldItems(SchemaDto.TableDto table) {
        if (table.fields().isEmpty()) {
            return List.of(tableMarker(table.name()));
        }
        return table.fields().stream()
                .map(f -> {
                    String path = table.name() + "." + f.name();
                    String id = UUID.nameUUIDFromBytes(path.getBytes(StandardCharsets.UTF_8)).toString();
                    return new FieldItem(
                            id,
                            SlugUtils.toSlug(f.name()),
                            f.name(),
                            f.type(),
                            path,
                            table.name(),
                            null,
                            Boolean.TRUE.equals(f.nullable()),
                            Boolean.TRUE.equals(f.pk()),
                            null
                    );
                })
                .toList();
    }

    private FieldItem tableMarker(String tableName) {
        String id = UUID.nameUUIDFromBytes(("table:" + tableName).getBytes(StandardCharsets.UTF_8)).toString();
        return new FieldItem(
                id,
                SlugUtils.toSlug(tableName),
                tableName,
                TABLE_MARKER_DATA_TYPE,
                tableName,
                null,
                null,
                true,
                false,
                null
        );
    }

    private String serializeFields(List<FieldItem> fields) {
        try {
            return objectMapper.writeValueAsString(fields);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize fieldData", e);
        }
    }

    private void applyRequest(MetaSync e, MetaSyncRequest req) {
        e.setStatus(req.status());
        e.setMetaSource(req.dataSourceId() == null ? null : loadSource(req.dataSourceId()));
        e.setMetaCode(req.metaCode());
        e.setMetaName(req.metaName());
        e.setFieldData(req.fieldData());
        e.setFieldHash(req.fieldHash());
        e.setDeleted(req.deleted() == null ? Boolean.FALSE : req.deleted());
        e.setActive(req.isActive() == null ? Boolean.TRUE : req.isActive());
        e.setVersionNo(req.versionNo());
        e.setChangedStatus(req.changedStatus());
        e.setChangedSummary(req.changedSummary());
    }

    private void deactivatePeersWhenActive(MetaSync e, UUID currentId) {
        if (!Boolean.TRUE.equals(e.getActive())
                || e.getMetaSource() == null
                || e.getMetaCode() == null
                || e.getMetaCode().isBlank()) {
            return;
        }
        if (currentId == null) {
            repo.deactivateAll(e.getMetaSource().getId(), e.getMetaCode());
        } else {
            repo.deactivateAllExcept(e.getMetaSource().getId(), e.getMetaCode(), currentId);
        }
        e.setActive(Boolean.TRUE);
    }

    private String buildChangedSummary(String changedStatus, String sourceName, String tableName, int fieldCount) {
        return switch (changedStatus) {
            case "CRITICAL" -> "Critical change in " + tableName + " from " + sourceName + " (" + fieldCount + " fields)";
            default -> "Warning: " + tableName + " changed in " + sourceName + " (" + fieldCount + " fields)";
        };
    }

    private String computeChangedStatus(String oldFieldDataJson, List<FieldItem> newFields) {
        if (oldFieldDataJson == null || oldFieldDataJson.isBlank()) {
            return "WARNING";
        }
        try {
            List<FieldItem> oldFields = objectMapper.readValue(oldFieldDataJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, FieldItem.class));
            Map<String, String> oldTypeByName = oldFields.stream()
                    .filter(field -> !isTableMarker(field))
                    .collect(Collectors.toMap(FieldItem::name, FieldItem::dataType, (a, b) -> a));
            Set<String> newNames = newFields.stream()
                    .filter(field -> !isTableMarker(field))
                    .map(FieldItem::name)
                    .collect(Collectors.toSet());
            // Field bị xóa → CRITICAL
            for (String oldName : oldTypeByName.keySet()) {
                if (!newNames.contains(oldName)) return "CRITICAL";
            }
            // Type thay đổi → CRITICAL
            for (FieldItem f : newFields) {
                if (isTableMarker(f)) continue;
                String oldType = oldTypeByName.get(f.name());
                if (oldType != null && !oldType.equals(f.dataType())) return "CRITICAL";
            }
            // Chỉ thêm field → WARNING
            return "WARNING";
        } catch (Exception e) {
            return "CRITICAL";
        }
    }

    private String resolveMetaSyncCode(MetaSource source) {
        if (source == null || source.getId() == null) {
            throw new IllegalStateException("MetaSource is required for MetaSync");
        }
        return repo.findFirstNumericCodeByMetaSourceId(source.getId())
                .orElseGet(this::generateNextNumericCode);
    }

    private boolean isTableMarker(FieldItem field) {
        return field != null && TABLE_MARKER_DATA_TYPE.equals(field.dataType());
    }



    private int safeVersionNo(Integer versionNo) {
        return versionNo == null ? 1 : versionNo;
    }

    private int nextVersionNo(Integer versionNo) {
        return safeVersionNo(versionNo) + 1;
    }

    private String generateNextNumericCode() {
        int next = repo.findMaxNumericCode() + 1;
        String candidate;
        do {
            candidate = String.format("%05d", next);
            next++;
        } while (repo.existsByCode(candidate));
        return candidate;
    }

    private MetaSync loadOrThrow(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("MetaSync not found: " + id));
    }

    private MetaSource loadSource(UUID id) {
        return sourceRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("MetaSource not found: " + id));
    }
}
