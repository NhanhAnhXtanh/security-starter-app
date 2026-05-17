package com.react.spring.catalog.service;

import com.react.spring.catalog.dto.OrganizationDto;
import com.react.spring.catalog.dto.OrganizationRequest;
import com.react.spring.catalog.entity.Organization;
import com.react.spring.common.exception.NotFoundException;
import com.react.spring.catalog.mapper.CatalogMappers;
import com.react.spring.catalog.repository.OrganizationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class OrganizationService {

    private final OrganizationRepository repo;

    public OrganizationService(OrganizationRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public Page<OrganizationDto> list(Pageable pageable) {
        return repo.findAll(pageable).map(CatalogMappers::toDto);
    }

    @Transactional(readOnly = true)
    public OrganizationDto getById(UUID id) {
        return CatalogMappers.toDto(loadOrThrow(id));
    }

    public OrganizationDto create(OrganizationRequest req) {
        Organization e = new Organization();
        CatalogMappers.apply(e, req);
        return CatalogMappers.toDto(repo.save(e));
    }

    public OrganizationDto update(UUID id, OrganizationRequest req) {
        Organization e = loadOrThrow(id);
        CatalogMappers.apply(e, req);
        return CatalogMappers.toDto(repo.save(e));
    }

    public void delete(UUID id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Organization not found: " + id);
        }
        repo.deleteById(id);
    }

    private Organization loadOrThrow(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Organization not found: " + id));
    }
}
