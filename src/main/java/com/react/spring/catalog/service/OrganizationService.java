package com.react.spring.catalog.service;

import com.react.spring.catalog.dto.OrganizationDto;
import com.react.spring.catalog.dto.OrganizationRequest;
import com.react.spring.catalog.entity.Organization;
import com.react.spring.catalog.mapper.CatalogMappers;
import com.react.spring.common.exception.NotFoundException;
import com.vn.security.core.security.data.SecureDataManager;
import com.vn.security.core.security.data.SecureDataManager.EntityMutation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

// Distinct bean name — starter ships its own com.vn.security.core.service.OrganizationService.
@Service("appOrganizationService")
@Transactional
public class OrganizationService {

    private static final Class<Organization> ENTITY_CLASS = Organization.class;
    private static final List<String> WRITABLE_ATTRS = List.of("name", "description");

    private final SecureDataManager secureDataManager;

    public OrganizationService(SecureDataManager secureDataManager) {
        this.secureDataManager = secureDataManager;
    }

    @Transactional(readOnly = true)
    public Page<OrganizationDto> list(Pageable pageable) {
        return secureDataManager.loadList(ENTITY_CLASS, pageable).map(CatalogMappers::toDto);
    }

    @Transactional(readOnly = true)
    public OrganizationDto getById(UUID id) {
        return CatalogMappers.toDto(loadOrThrow(id));
    }

    public OrganizationDto create(OrganizationRequest req) {
        Organization e = new Organization();
        CatalogMappers.apply(e, req);
        Organization saved = secureDataManager.save(ENTITY_CLASS, null, new EntityMutation<>(e, WRITABLE_ATTRS));
        return CatalogMappers.toDto(saved);
    }

    public OrganizationDto update(UUID id, OrganizationRequest req) {
        Organization e = loadOrThrow(id);
        CatalogMappers.apply(e, req);
        Organization saved = secureDataManager.save(ENTITY_CLASS, id, new EntityMutation<>(e, WRITABLE_ATTRS));
        return CatalogMappers.toDto(saved);
    }

    public void delete(UUID id) {
        secureDataManager.delete(ENTITY_CLASS, id);
    }

    private Organization loadOrThrow(UUID id) {
        return secureDataManager.loadOne(ENTITY_CLASS, id)
                .orElseThrow(() -> new NotFoundException("Organization not found: " + id));
    }
}
