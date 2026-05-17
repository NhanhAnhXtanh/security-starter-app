package com.react.spring.meta.metasource.service;

import com.react.spring.meta.metasource.dto.MetaSourceDto;
import com.react.spring.meta.metasource.dto.MetaSourceRequest;
import com.react.spring.catalog.entity.Domain;
import com.react.spring.catalog.entity.Organization;
import com.react.spring.meta.metasource.entity.MetaSource;
import com.react.spring.common.exception.NotFoundException;
import com.react.spring.meta.metasource.mapper.MetaSourceMapper;
import com.vn.security.core.security.data.SecureDataManager;
import com.vn.security.core.security.data.SecureDataManager.EntityMutation;
import com.vn.security.core.security.data.UnconstrainedDataManager;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class MetaSourceService {

    private static final Class<MetaSource> ENTITY_CLASS = MetaSource.class;
    private static final List<String> WRITABLE_ATTRS = List.of(
        "code", "name", "sourceType", "connectorType", "description",
        "enabled", "organization", "domain", "connectorConfig"
    );

    private final SecureDataManager secureDataManager;
    // Bypass: by-field lookups (code, name) not in SecureDataManager.
    private final UnconstrainedDataManager unconstrainedDataManager;
    // EntityManager: PostgreSQL native regex+CAST for findMaxNumericCode.
    private final EntityManager entityManager;

    public MetaSourceService(SecureDataManager secureDataManager,
                             UnconstrainedDataManager unconstrainedDataManager,
                             EntityManager entityManager) {
        this.secureDataManager = secureDataManager;
        this.unconstrainedDataManager = unconstrainedDataManager;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public Page<MetaSourceDto> list(Pageable pageable) {
        return secureDataManager.loadList(ENTITY_CLASS, pageable).map(MetaSourceMapper::toDto);
    }

    @Transactional(readOnly = true)
    public MetaSourceDto getById(UUID id) {
        return MetaSourceMapper.toDto(loadByIdOrThrow(id));
    }

    @Transactional(readOnly = true)
    public MetaSourceDto getByCode(String code) {
        MetaSource e = findByCode(code)
                .orElseThrow(() -> new NotFoundException("MetaSource not found: " + code));
        return MetaSourceMapper.toDto(e);
    }

    public MetaSourceDto create(MetaSourceRequest req) {
        if (existsByName(req.name())) {
            throw new IllegalArgumentException("MetaSource name already exists: " + req.name());
        }
        MetaSource e = new MetaSource();
        e.setCode(generateNextNumericCode());
        applyRequest(e, req);
        MetaSource saved = secureDataManager.save(ENTITY_CLASS, null, new EntityMutation<>(e, WRITABLE_ATTRS));
        return MetaSourceMapper.toDto(saved);
    }

    public MetaSourceDto update(UUID id, MetaSourceRequest req) {
        MetaSource e = loadByIdOrThrow(id);
        if (!e.getName().equals(req.name()) && existsByName(req.name())) {
            throw new IllegalArgumentException("MetaSource name already exists: " + req.name());
        }
        applyRequest(e, req);
        MetaSource saved = secureDataManager.save(ENTITY_CLASS, id, new EntityMutation<>(e, WRITABLE_ATTRS));
        return MetaSourceMapper.toDto(saved);
    }

    public void delete(UUID id) {
        secureDataManager.delete(ENTITY_CLASS, id);
    }

    private void applyRequest(MetaSource e, MetaSourceRequest req) {
        if (req.connectorType().getSourceType() != req.sourceType()) {
            throw new IllegalArgumentException(
                    "Connector " + req.connectorType() + " does not belong to source type " + req.sourceType()
            );
        }
        e.setName(req.name());
        e.setSourceType(req.sourceType());
        e.setConnectorType(req.connectorType());
        e.setDescription(req.description());
        e.setEnabled(req.enabled() == null ? Boolean.TRUE : req.enabled());
        e.setOrganization(req.organizationId() == null ? null : loadOrganization(req.organizationId()));
        e.setDomain(req.domainId() == null ? null : loadDomain(req.domainId()));
        e.setConnectorConfig(req.connectorConfig());
    }

    private Optional<MetaSource> findByCode(String code) {
        List<MetaSource> r = unconstrainedDataManager.loadListByJpql(
                ENTITY_CLASS,
                "select m from MetaSource m where m.code = :code",
                Map.of("code", code), null);
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }

    private boolean existsByName(String name) {
        return !unconstrainedDataManager.loadListByJpql(
                ENTITY_CLASS,
                "select m from MetaSource m where m.name = :name",
                Map.of("name", name), null).isEmpty();
    }

    private boolean existsByCode(String code) {
        return findByCode(code).isPresent();
    }

    private int findMaxNumericCode() {
        Object r = entityManager.createNativeQuery(
                "SELECT COALESCE(MAX(CAST(code AS INTEGER)), 0) FROM core_meta_source WHERE code ~ '^[0-9]+$'"
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

    private MetaSource loadByIdOrThrow(UUID id) {
        return secureDataManager.loadOne(ENTITY_CLASS, id)
                .orElseThrow(() -> new NotFoundException("MetaSource not found: " + id));
    }

    private Organization loadOrganization(UUID id) {
        return secureDataManager.loadOne(Organization.class, id)
                .orElseThrow(() -> new NotFoundException("Organization not found: " + id));
    }

    private Domain loadDomain(UUID id) {
        return secureDataManager.loadOne(Domain.class, id)
                .orElseThrow(() -> new NotFoundException("Domain not found: " + id));
    }
}
