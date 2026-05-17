package com.react.spring.meta.metapack.service;

import com.react.spring.meta.metapack.dto.MetaPackDto;
import com.react.spring.meta.metapack.dto.MetaPackMapper;
import com.react.spring.meta.metapack.entity.MetaPack;
import com.react.spring.meta.metapack.entity.MetaPackVersion;
import com.react.spring.meta.metapack.dto.MetaPackVersionItemDto;
import com.react.spring.meta.metapack.dto.MetaPackVersionDto;
import com.react.spring.meta.metapack.repository.MetaPackRepository;
import com.react.spring.meta.metapack.repository.MetaPackVersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class MetaPackService {
    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Autowired
    private MetaPackRepository metaPackRepository;

    @Autowired
    private MetaPackMapper metaPackMapper;

    @Autowired
    private MetaPackVersionRepository metaPackVersionRepository;

    @Transactional(readOnly = true)
    public List<MetaPackDto> findAll() {
        return metaPackRepository.findAll().stream()
                .map(metaPackMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<MetaPackDto> findById(UUID id) {
        return metaPackRepository.findById(id).map(metaPackMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<MetaPackDto> findByIdOrCode(String identifier) {
        try {
            UUID id = UUID.fromString(identifier);
            return findById(id);
        } catch (IllegalArgumentException e) {
            return metaPackRepository.findByCode(identifier).map(metaPackMapper::toDto);
        }
    }

    @Transactional(readOnly = true)
    public List<MetaPackVersionDto> listVersions(UUID metaPackId) {
        return metaPackVersionRepository.findByMetaPackIdOrderByVersionNumberDesc(metaPackId).stream()
                .map(this::toVersionDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public MetaPackDto create(MetaPackDto dto) {
        if (dto.getName() == null) {
            throw new IllegalArgumentException("Name is required");
        }
        MetaPack entity = new MetaPack();

        // Business code is server-generated in sequential numeric format.
        entity.setCode(generateNextNumericCode());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setMaxRequestsPerMinute(dto.getMaxRequestsPerMinute() != null ? dto.getMaxRequestsPerMinute() : 60);
        entity.setMaxRequestsPerDay(dto.getMaxRequestsPerDay() != null ? dto.getMaxRequestsPerDay() : 10000);
        entity.setStatus("DRAFT");
        
        MetaPack saved = metaPackRepository.save(entity);

        // Always create initial version snapshot so MetaPack starts with a version record.
        MetaPackVersion version = new MetaPackVersion();
        version.setMetaPack(saved);
        version.setVersionNumber(nextBusinessVersionNumber(null));
        version.setStatus("DRAFT");
        String configJson = serializeVersionItems(dto.getVersionItems());
        version.setDataConfig(configJson);
        version.setDataHash(hashJson(configJson));
        version = metaPackVersionRepository.save(version);
        saved.setCurrentVersion(version);
        saved = metaPackRepository.save(saved);

        return metaPackMapper.toDto(saved);
    }

    @Transactional
    public MetaPackDto updateByIdentifier(String identifier, MetaPackDto dto) {
        MetaPack entity;
        try {
            UUID id = UUID.fromString(identifier);
            entity = metaPackRepository.findById(id)
                    .orElseGet(() -> metaPackRepository.findByCode(identifier)
                            .orElseThrow(() -> new IllegalArgumentException("MetaPack not found: " + identifier)));
        } catch (IllegalArgumentException e) {
            entity = metaPackRepository.findByCode(identifier)
                    .orElseThrow(() -> new IllegalArgumentException("MetaPack not found: " + identifier));
        }
        
        return updateInternal(entity, dto);
    }

    @Transactional
    public MetaPackDto update(UUID id, MetaPackDto dto) {
        MetaPack entity = metaPackRepository.findById(id)
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

            boolean configChanged = hasBusinessConfigChanged(currentVersion, newDataHash);

            if (configChanged) {
                MetaPackVersion newVersion = new MetaPackVersion();
                newVersion.setMetaPack(entity);
                newVersion.setVersionNumber(nextBusinessVersionNumber(currentVersion));
                newVersion.setStatus("DRAFT");
                newVersion.setDataConfig(newConfigJson);
                newVersion.setDataHash(newDataHash);

                newVersion = metaPackVersionRepository.save(newVersion);
                entity.setCurrentVersion(newVersion);

                if ("PUBLISHED".equals(oldStatus)) {
                    entity.setStatus("DRAFT");
                }
            }

            MetaPack saved = metaPackRepository.save(entity);
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
        metaPackRepository.deleteById(id);
    }

    private String generateNextNumericCode() {
        int nextValue = metaPackRepository.findMaxNumericCode() + 1;
        String candidate;
        do {
            candidate = String.format("%06d", nextValue++);
        } while (metaPackRepository.existsByCode(candidate));
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
