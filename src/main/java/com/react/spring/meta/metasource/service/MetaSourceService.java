package com.react.spring.meta.metasource.service;

import com.react.spring.meta.metasource.dto.MetaSourceDto;
import com.react.spring.meta.metasource.dto.MetaSourceRequest;
import com.react.spring.catalog.entity.Domain;
import com.react.spring.catalog.entity.Organization;
import com.react.spring.meta.metasource.entity.MetaSource;
import com.react.spring.common.exception.NotFoundException;
import com.react.spring.meta.metasource.mapper.MetaSourceMapper;
import com.react.spring.catalog.repository.DomainRepository;
import com.react.spring.catalog.repository.OrganizationRepository;
import com.react.spring.meta.metasource.repository.MetaSourceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class MetaSourceService {

    private final MetaSourceRepository repo;
    private final OrganizationRepository orgRepo;
    private final DomainRepository domainRepo;

    public MetaSourceService(MetaSourceRepository repo,
                             OrganizationRepository orgRepo,
                             DomainRepository domainRepo) {
        this.repo = repo;
        this.orgRepo = orgRepo;
        this.domainRepo = domainRepo;
    }

    @Transactional(readOnly = true)
    public Page<MetaSourceDto> list(Pageable pageable) {
        return repo.findAll(pageable).map(MetaSourceMapper::toDto);
    }

    @Transactional(readOnly = true)
    public MetaSourceDto getById(UUID id) {
        return MetaSourceMapper.toDto(loadByIdOrThrow(id));
    }

    @Transactional(readOnly = true)
    public MetaSourceDto getByCode(String code) {
        MetaSource e = repo.findByCode(code)
                .orElseThrow(() -> new NotFoundException("MetaSource not found: " + code));
        return MetaSourceMapper.toDto(e);
    }

    public MetaSourceDto create(MetaSourceRequest req) {
        if (repo.existsByName(req.name())) {
            throw new IllegalArgumentException("MetaSource name already exists: " + req.name());
        }
        MetaSource e = new MetaSource();
        e.setCode(generateNextNumericCode());
        applyRequest(e, req);
        return MetaSourceMapper.toDto(repo.save(e));
    }

    public MetaSourceDto update(UUID id, MetaSourceRequest req) {
        MetaSource e = loadByIdOrThrow(id);
        if (!e.getName().equals(req.name()) && repo.existsByName(req.name())) {
            throw new IllegalArgumentException("MetaSource name already exists: " + req.name());
        }
        applyRequest(e, req);
        return MetaSourceMapper.toDto(repo.save(e));
    }

    public void delete(UUID id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("MetaSource not found: " + id);
        }
        repo.deleteById(id);
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



    private String generateNextNumericCode() {
        int next = repo.findMaxNumericCode() + 1;
        String candidate;
        do {
            candidate = String.format("%05d", next);
            next++;
        } while (repo.existsByCode(candidate));
        return candidate;
    }

    private MetaSource loadByIdOrThrow(UUID id) {
        return repo.findById(id)
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
}
