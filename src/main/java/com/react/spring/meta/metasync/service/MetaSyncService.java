package com.react.spring.meta.metasync.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.react.spring.meta.metasync.dto.FieldItem;
import com.react.spring.meta.metasync.dto.MetaSyncDto;
import com.react.spring.meta.metasync.dto.MetaSyncRequest;
import com.react.spring.meta.metasource.connect.db.dto.SchemaDto;
import com.react.spring.meta.metasource.connect.db.dto.SyncResultDto;
import com.react.spring.meta.metasource.entity.MetaSource;
import com.react.spring.meta.metaset.entity.MetaSet;
import com.react.spring.meta.metasetversion.entity.MetaSetVersion;
import com.react.spring.meta.metasync.entity.MetaSync;
import com.react.spring.common.exception.NotFoundException;
import com.react.spring.meta.metasync.mapper.MetaSyncMapper;
import com.react.spring.meta.metasource.connect.db.MetaSourceConnectionService;
import com.react.spring.common.utils.FieldHashUtils;
import com.react.spring.common.utils.SlugUtils;
import com.vn.security.core.security.data.SecureDataManager;
import com.vn.security.core.security.data.SecureDataManager.EntityMutation;
import com.vn.security.core.security.data.UnconstrainedDataManager;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

    private static final Class<MetaSync> ENTITY_CLASS = MetaSync.class;
    private static final List<String> WRITABLE_ATTRS = List.of(
        "code", "status", "metaSource", "metaCode", "metaName",
        "fieldData", "fieldHash", "deleted", "active",
        "versionNo", "changedStatus", "changedSummary"
    );

    private final SecureDataManager secureDataManager;
    // Bypass: starter has no Specification-based filter, no by-field lookups,
    // no @Modifying UPDATE/DELETE. All uses below are read-only system queries
    // OR writes that already passed an upstream SecureDataManager check
    // (per rules/data-access.md §2.3).
    private final UnconstrainedDataManager unconstrainedDataManager;
    // EntityManager: PostgreSQL native (regex/CAST for code), JPQL @Modifying UPDATE
    // for bulk deactivate (no equivalent in starter API).
    private final EntityManager entityManager;
    private final MetaSourceConnectionService connectionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MetaSyncService(SecureDataManager secureDataManager,
                           UnconstrainedDataManager unconstrainedDataManager,
                           EntityManager entityManager,
                           MetaSourceConnectionService connectionService) {
        this.secureDataManager = secureDataManager;
        this.unconstrainedDataManager = unconstrainedDataManager;
        this.entityManager = entityManager;
        this.connectionService = connectionService;
    }

    @Transactional(readOnly = true)
    public Page<MetaSyncDto> list(UUID dataSourceId, UUID organizationId, UUID domainId, String keyword, Pageable pageable) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Specification<MetaSync> spec = (root, query, cb) -> cb.equal(root.get("active"), Boolean.TRUE);
        if (dataSourceId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("metaSource").get("id"), dataSourceId));
        }
        if (organizationId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("metaSource").get("organization").get("id"), organizationId));
        }
        if (domainId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("metaSource").get("domain").get("id"), domainId));
        }
        if (kw != null) {
            String like = "%" + kw.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("metaName")), like),
                    cb.like(cb.lower(root.get("code")), like)
            ));
        }
        return unconstrainedDataManager.loadPage(ENTITY_CLASS, spec, pageable).map(MetaSyncMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<MetaSyncDto> listBySource(UUID metaSourceId) {
        return unconstrainedDataManager.loadListByJpql(
                ENTITY_CLASS,
                "select s from MetaSync s where s.metaSource.id = :id and s.active = true order by s.metaCode asc",
                Map.of("id", metaSourceId), null)
                .stream().map(MetaSyncMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public MetaSyncDto getById(UUID id) {
        return MetaSyncMapper.toDto(loadOrThrow(id));
    }

    @Transactional(readOnly = true)
    public MetaSyncDto getByCode(String code) {
        List<MetaSync> r = unconstrainedDataManager.loadListByJpql(
                ENTITY_CLASS,
                "select s from MetaSync s where s.code = :code and s.active = true order by s.metaCode asc, s.versionNo desc",
                Map.of("code", code), null);
        MetaSync e = r.isEmpty() ? null : r.get(0);
        if (e == null) throw new NotFoundException("MetaSync not found: " + code);
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
        MetaSync saved = secureDataManager.save(ENTITY_CLASS, null, new EntityMutation<>(e, WRITABLE_ATTRS));
        return MetaSyncMapper.toDto(saved);
    }

    public MetaSyncDto update(UUID id, MetaSyncRequest req) {
        MetaSync e = loadOrThrow(id);
        applyRequest(e, req);
        deactivatePeersWhenActive(e, id);
        MetaSync saved = secureDataManager.save(ENTITY_CLASS, id, new EntityMutation<>(e, WRITABLE_ATTRS));
        return MetaSyncMapper.toDto(saved);
    }

    public void delete(UUID id) {
        MetaSync sync = loadOrThrow(id);
        String metaSyncCode = sync.getCode();

        // Cascade delete: remove all MetaSets and MetaSetVersions extracted from this MetaSync.
        // System-internal bulk delete — JPA @Modifying via EntityManager.
        if (metaSyncCode != null && !metaSyncCode.isBlank()) {
            entityManager.createQuery(
                    "DELETE FROM MetaSetVersion v WHERE v.metasyncCode = :metasyncCode")
                .setParameter("metasyncCode", metaSyncCode)
                .executeUpdate();
            entityManager.createQuery(
                    """
                    DELETE FROM MetaSet ms
                    WHERE ms.id IN (
                        SELECT DISTINCT ms2.id
                        FROM MetaSet ms2
                        LEFT JOIN ms2.currentVersion currentVersion
                        WHERE currentVersion.metasyncCode = :metasyncCode
                           OR EXISTS (
                               SELECT 1
                               FROM MetaSetVersion version
                                WHERE version.metaCode = ms2.metaCode
                                  AND version.metasyncCode = :metasyncCode
                           )
                    )
                    """)
                .setParameter("metasyncCode", metaSyncCode)
                .executeUpdate();
            entityManager.clear();
        }

        secureDataManager.delete(ENTITY_CLASS, id);
    }

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

            MetaSync latest = findLatestByMetaSourceAndMetaCode(metaSourceId, table.name()).orElse(null);

            if (latest != null
                    && !Boolean.TRUE.equals(latest.getDeleted())
                    && Objects.equals(latest.getFieldHash(), structuralHash)) {
                if (!Boolean.TRUE.equals(latest.getActive()) || STATUS_OFFLINE.equals(latest.getStatus())) {
                    latest.setActive(Boolean.TRUE);
                    latest.setStatus(STATUS_ACTIVE);
                    // System write: refresh of existing record during sync — bypass since
                    // initSync runs as a system job (scheduled poll / admin trigger).
                    unconstrainedDataManager.save(latest);
                }
                skipped++;
                continue;
            }

            int versionNo = latest == null ? 1 : nextVersionNo(latest.getVersionNo());
            String changedStatus = latest == null
                    ? "WARNING"
                    : Boolean.TRUE.equals(latest.getDeleted()) ? "WARNING"
                    : computeChangedStatus(latest.getFieldData(), fields);

            deactivateAll(metaSourceId, table.name());

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

            changedItems.add(MetaSyncMapper.toDto(unconstrainedDataManager.save(e)));
        }

        // Detect bảng bị xóa
        for (MetaSync latest : findLatestPerMetaCode(metaSourceId)) {
            String metaCode = latest.getMetaCode();
            if (metaCode == null || currentTables.containsKey(metaCode) || Boolean.TRUE.equals(latest.getDeleted())) {
                continue;
            }

            deactivateAll(metaSourceId, metaCode);

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
            changedItems.add(MetaSyncMapper.toDto(unconstrainedDataManager.save(deleted)));
        }

        return new SyncResultDto(changedItems.size(), skipped, changedItems, SYNC_MODE_ONLINE, null);
    }

    private SyncResultDto useOfflineMetaSync(UUID metaSourceId, RuntimeException cause) {
        List<MetaSync> latestItems = unconstrainedDataManager.loadListByJpql(
                ENTITY_CLASS,
                "select s from MetaSync s where s.metaSource.id = :id and s.active = true order by s.metaCode asc",
                Map.of("id", metaSourceId), null);
        List<MetaSyncDto> items = latestItems.stream()
                .map(item -> {
                    if (!STATUS_OFFLINE.equals(item.getStatus())) {
                        item.setStatus(STATUS_OFFLINE);
                        unconstrainedDataManager.save(item);
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
            deactivateAll(e.getMetaSource().getId(), e.getMetaCode());
        } else {
            deactivateAllExcept(e.getMetaSource().getId(), e.getMetaCode(), currentId);
        }
        e.setActive(Boolean.TRUE);
    }

    private void deactivateAll(UUID metaSourceId, String metaCode) {
        entityManager.createQuery(
                "UPDATE MetaSync ms SET ms.active = false " +
                "WHERE ms.metaSource.id = :metaSourceId AND ms.metaCode = :metaCode")
            .setParameter("metaSourceId", metaSourceId)
            .setParameter("metaCode", metaCode)
            .executeUpdate();
        entityManager.clear();
    }

    private void deactivateAllExcept(UUID metaSourceId, String metaCode, UUID id) {
        entityManager.createQuery(
                "UPDATE MetaSync ms SET ms.active = false " +
                "WHERE ms.metaSource.id = :metaSourceId AND ms.metaCode = :metaCode AND ms.id <> :id")
            .setParameter("metaSourceId", metaSourceId)
            .setParameter("metaCode", metaCode)
            .setParameter("id", id)
            .executeUpdate();
        entityManager.clear();
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
            for (String oldName : oldTypeByName.keySet()) {
                if (!newNames.contains(oldName)) return "CRITICAL";
            }
            for (FieldItem f : newFields) {
                if (isTableMarker(f)) continue;
                String oldType = oldTypeByName.get(f.name());
                if (oldType != null && !oldType.equals(f.dataType())) return "CRITICAL";
            }
            return "WARNING";
        } catch (Exception e) {
            return "CRITICAL";
        }
    }

    private String resolveMetaSyncCode(MetaSource source) {
        if (source == null || source.getId() == null) {
            throw new IllegalStateException("MetaSource is required for MetaSync");
        }
        return findFirstNumericCodeByMetaSourceId(source.getId())
                .orElseGet(this::generateNextNumericCode);
    }

    private Optional<String> findFirstNumericCodeByMetaSourceId(UUID metaSourceId) {
        @SuppressWarnings("unchecked")
        List<String> r = entityManager.createNativeQuery(
                """
                SELECT ms.code
                FROM core_meta_sync ms
                WHERE ms.data_source_id = :metaSourceId
                  AND ms.code ~ '^[0-9]+$'
                ORDER BY ms.created_date ASC NULLS LAST, ms.code ASC
                LIMIT 1
                """)
            .setParameter("metaSourceId", metaSourceId)
            .getResultList();
        return r.isEmpty() ? Optional.empty() : Optional.ofNullable(r.get(0));
    }

    private Optional<MetaSync> findLatestByMetaSourceAndMetaCode(UUID metaSourceId, String metaCode) {
        List<MetaSync> r = unconstrainedDataManager.loadListByJpql(
                ENTITY_CLASS,
                "select s from MetaSync s where s.metaSource.id = :id and s.metaCode = :code order by s.versionNo desc",
                Map.of("id", metaSourceId, "code", metaCode), null);
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }

    private List<MetaSync> findLatestPerMetaCode(UUID metaSourceId) {
        return unconstrainedDataManager.loadListByJpql(
                ENTITY_CLASS,
                "SELECT ms FROM MetaSync ms WHERE ms.metaSource.id = :metaSourceId " +
                "AND ms.versionNo = (SELECT MAX(ms2.versionNo) FROM MetaSync ms2 " +
                "                    WHERE ms2.metaSource.id = :metaSourceId AND ms2.metaCode = ms.metaCode)",
                Map.of("metaSourceId", metaSourceId), null);
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

    private boolean existsByCode(String code) {
        return !unconstrainedDataManager.loadListByJpql(
                ENTITY_CLASS,
                "select s from MetaSync s where s.code = :code",
                Map.of("code", code), null).isEmpty();
    }

    private int findMaxNumericCode() {
        Object r = entityManager.createNativeQuery(
                "SELECT COALESCE(MAX(CAST(code AS INTEGER)), 0) FROM core_meta_sync WHERE code ~ '^[0-9]+$'"
            ).getSingleResult();
        return r == null ? 0 : ((Number) r).intValue();
    }

    private String generateNextNumericCode() {
        int next = findMaxNumericCode() + 1;
        String candidate;
        do {
            candidate = String.format("%05d", next);
            next++;
        } while (existsByCode(candidate));
        return candidate;
    }

    private MetaSync loadOrThrow(UUID id) {
        return secureDataManager.loadOne(ENTITY_CLASS, id)
                .orElseThrow(() -> new NotFoundException("MetaSync not found: " + id));
    }

    private MetaSource loadSource(UUID id) {
        return secureDataManager.loadOne(MetaSource.class, id)
                .orElseThrow(() -> new NotFoundException("MetaSource not found: " + id));
    }
}
