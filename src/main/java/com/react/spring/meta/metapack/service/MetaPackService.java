package com.react.spring.meta.metapack.service;

import com.react.spring.meta.metapack.dto.MetaPackDto;
import com.react.spring.meta.metapack.dto.MetaPackMapper;
import com.react.spring.meta.metapack.dto.MetaPackVersionDto;
import com.react.spring.meta.metapack.dto.MetaPackVersionItemDto;
import com.react.spring.meta.metapack.entity.MetaPack;
import com.react.spring.meta.metapack.entity.MetaPackVersion;
import com.vn.security.core.security.data.SecureDataManager;
import com.vn.security.core.security.data.SecureDataManager.EntityMutation;
import com.vn.security.core.security.data.UnconstrainedDataManager;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MetaPackService {

    private static final Class<MetaPack> ENTITY_CLASS = MetaPack.class;
    private static final List<String> WRITABLE_ATTRS = List.of(
        "code", "name", "description", "status",
        "maxRequestsPerMinute", "maxRequestsPerDay", "currentVersion"
    );

    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final SecureDataManager secureDataManager;
    // Bypass: starter SecureDataManager only supports lookup by id; non-id lookups
    // (findByCode, exists, findMaxNumericCode native query, findByMetaPackId order-by)
    // go through Unconstrained per rules/data-access.md §2.3 — service still gates by id
    // via secureDataManager for the actual CRUD path.
    private final UnconstrainedDataManager unconstrainedDataManager;
    // EntityManager: native PostgreSQL query (regex + CAST) — no JPQL equivalent.
    private final EntityManager entityManager;

    @Autowired
    private MetaPackMapper metaPackMapper;

    public MetaPackService(
        SecureDataManager secureDataManager,
        UnconstrainedDataManager unconstrainedDataManager,
        EntityManager entityManager
    ) {
        this.secureDataManager = secureDataManager;
        this.unconstrainedDataManager = unconstrainedDataManager;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<MetaPackDto> findAll() {
        return secureDataManager.loadList(ENTITY_CLASS, Pageable.unpaged())
                .stream()
                .map(metaPackMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<MetaPackDto> findById(UUID id) {
        return secureDataManager.loadOne(ENTITY_CLASS, id).map(metaPackMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<MetaPackDto> findByIdOrCode(String identifier) {
        try {
            UUID id = UUID.fromString(identifier);
            return findById(id);
        } catch (IllegalArgumentException e) {
            return findByCode(identifier).map(metaPackMapper::toDto);
        }
    }

    @Transactional(readOnly = true)
    public List<MetaPackVersionDto> listVersions(UUID metaPackId) {
        // Bypass: order-by lookup not in SecureDataManager API.
        List<MetaPackVersion> versions = unconstrainedDataManager.loadListByJpql(
            MetaPackVersion.class,
            "select v from MetaPackVersion v where v.metaPack.id = :id order by v.versionNumber desc",
            Map.of("id", metaPackId),
            null
        );
        return versions.stream().map(this::toVersionDto).collect(Collectors.toList());
    }

    @Transactional
    public MetaPackDto create(MetaPackDto dto) {
        if (dto.getName() == null) {
            throw new IllegalArgumentException("Name is required");
        }
        MetaPack entity = new MetaPack();
        entity.setCode(generateNextNumericCode());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setMaxRequestsPerMinute(dto.getMaxRequestsPerMinute() != null ? dto.getMaxRequestsPerMinute() : 60);
        entity.setMaxRequestsPerDay(dto.getMaxRequestsPerDay() != null ? dto.getMaxRequestsPerDay() : 10000);
        entity.setStatus("DRAFT");

        // Initial version snapshot — cascaded via MetaPack.currentVersion (CascadeType.ALL).
        MetaPackVersion version = new MetaPackVersion();
        version.setMetaPack(entity);
        version.setVersionNumber(nextBusinessVersionNumber(null));
        version.setStatus("DRAFT");
        String configJson = serializeVersionItems(dto.getVersionItems());
        version.setDataConfig(configJson);
        version.setDataHash(hashJson(configJson));
        entity.setCurrentVersion(version);

        MetaPack saved = secureDataManager.save(ENTITY_CLASS, null, new EntityMutation<>(entity, WRITABLE_ATTRS));
        return metaPackMapper.toDto(saved);
    }

    @Transactional
    public MetaPackDto updateByIdentifier(String identifier, MetaPackDto dto) {
        MetaPack entity;
        try {
            UUID id = UUID.fromString(identifier);
            entity = secureDataManager.loadOne(ENTITY_CLASS, id)
                    .orElseGet(() -> findByCode(identifier)
                            .orElseThrow(() -> new IllegalArgumentException("MetaPack not found: " + identifier)));
        } catch (IllegalArgumentException e) {
            entity = findByCode(identifier)
                    .orElseThrow(() -> new IllegalArgumentException("MetaPack not found: " + identifier));
        }
        return updateInternal(entity, dto);
    }

    @Transactional
    public MetaPackDto update(UUID id, MetaPackDto dto) {
        MetaPack entity = secureDataManager.loadOne(ENTITY_CLASS, id)
                .orElseThrow(() -> new IllegalArgumentException("MetaPack not found"));
        return updateInternal(entity, dto);
    }

    private MetaPackDto updateInternal(MetaPack entity, MetaPackDto dto) {
        try {
            String oldStatus = entity.getStatus();
            entity.setName(dto.getName());
            entity.setDescription(dto.getDescription());
            entity.setMaxRequestsPerMinute(dto.getMaxRequestsPerMinute());
            entity.setMaxRequestsPerDay(dto.getMaxRequestsPerDay());

            String newStatus = dto.getStatus();
            if (newStatus != null) {
                entity.setStatus(newStatus);
            }

            MetaPackVersion currentVersion = entity.getCurrentVersion();
            String newConfigJson = dto.getVersionItems() != null
                    ? serializeVersionItems(dto.getVersionItems())
                    : currentVersion != null && currentVersion.getDataConfig() != null
                        ? currentVersion.getDataConfig()
                        : "[]";
            String newDataHash = hashJson(newConfigJson);

            if (hasBusinessConfigChanged(currentVersion, newDataHash)) {
                MetaPackVersion newVersion = new MetaPackVersion();
                newVersion.setMetaPack(entity);
                newVersion.setVersionNumber(nextBusinessVersionNumber(currentVersion));
                newVersion.setStatus("DRAFT");
                newVersion.setDataConfig(newConfigJson);
                newVersion.setDataHash(newDataHash);
                // Bypass: save linked version directly — cascaded write via parent SecureDataManager.save
                // would not get a fresh id assigned before parent flushes. Comment: system-internal write
                // wrapped inside a user-initiated update transaction (still authorized via parent loadOne).
                newVersion = unconstrainedDataManager.save(newVersion);
                entity.setCurrentVersion(newVersion);

                if ("PUBLISHED".equals(oldStatus)) {
                    entity.setStatus("DRAFT");
                }
            }

            MetaPack saved = secureDataManager.save(ENTITY_CLASS, entity.getId(), new EntityMutation<>(entity, WRITABLE_ATTRS));
            return metaPackMapper.toDto(saved);
        } catch (Exception e) {
            StringBuilder message = new StringBuilder(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            Throwable cause = e;
            while (cause.getCause() != null && cause != cause.getCause()) {
                cause = cause.getCause();
                message.append(" (Cause: ")
                        .append(cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName())
                        .append(')');
            }
            throw new RuntimeException("Lỗi lưu MetaPack [" + entity.getCode() + "]: " + message, e);
        }
    }

    @Transactional
    public void delete(UUID id) {
        secureDataManager.delete(ENTITY_CLASS, id);
    }

    // ---- Internal helpers (system-context, not user-routed) ----

    private Optional<MetaPack> findByCode(String code) {
        // Bypass: code lookup not supported by SecureDataManager API.
        List<MetaPack> result = unconstrainedDataManager.loadListByJpql(
            ENTITY_CLASS,
            "select m from MetaPack m where m.code = :code",
            Map.of("code", code),
            null
        );
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    private boolean existsByCode(String code) {
        return findByCode(code).isPresent();
    }

    private int findMaxNumericCode() {
        // EntityManager direct use — PostgreSQL native regex + CAST has no JPQL equivalent.
        // Read-only system query; safe to bypass.
        Object result = entityManager.createNativeQuery(
                "SELECT COALESCE(MAX(CAST(code AS INTEGER)), 0) FROM meta_pack WHERE code ~ '^[0-9]+$'"
            ).getSingleResult();
        return result == null ? 0 : ((Number) result).intValue();
    }

    private String generateNextNumericCode() {
        int nextValue = findMaxNumericCode() + 1;
        String candidate;
        do {
            candidate = String.format("%06d", nextValue++);
        } while (existsByCode(candidate));
        return candidate;
    }

    private String serializeVersionItems(List<MetaPackVersionItemDto> items) {
        try {
            return objectMapper.writeValueAsString(items == null ? List.of() : items);
        } catch (Exception e) {
            return "[]";
        }
    }

    private MetaPackVersionDto toVersionDto(MetaPackVersion version) {
        MetaPackVersionDto dto = new MetaPackVersionDto();
        dto.setId(version.getId());
        dto.setVersionNumber(version.getVersionNumber());
        dto.setStatus(version.getStatus());
        dto.setReleaseNotes(version.getReleaseNotes());
        dto.setDataConfig(version.getDataConfig());
        dto.setDataHash(version.getDataHash());
        dto.setCreatedBy(version.getCreatedBy());
        dto.setCreatedAt(version.getCreatedDate());
        return dto;
    }

    private String hashJson(String json) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest((json == null ? "[]" : json).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private boolean hasBusinessConfigChanged(MetaPackVersion currentVersion, String newDataHash) {
        return currentVersion == null
                || currentVersion.getDataHash() == null
                || !Objects.equals(newDataHash, currentVersion.getDataHash());
    }

    private int nextBusinessVersionNumber(MetaPackVersion currentVersion) {
        if (currentVersion == null || currentVersion.getVersionNumber() == null) {
            return 1;
        }
        return currentVersion.getVersionNumber() + 1;
    }
}
