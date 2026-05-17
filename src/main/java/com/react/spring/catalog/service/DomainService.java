package com.react.spring.catalog.service;

import com.react.spring.catalog.dto.DomainDto;
import com.react.spring.catalog.dto.DomainRequest;
import com.react.spring.catalog.entity.Domain;
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

@Service
@Transactional
public class DomainService {

    private static final Class<Domain> ENTITY_CLASS = Domain.class;
    private static final List<String> WRITABLE_ATTRS = List.of("name", "description");

    private final SecureDataManager secureDataManager;

    public DomainService(SecureDataManager secureDataManager) {
        this.secureDataManager = secureDataManager;
    }

    @Transactional(readOnly = true)
    public Page<DomainDto> list(Pageable pageable) {
        return secureDataManager.loadList(ENTITY_CLASS, pageable).map(CatalogMappers::toDto);
    }

    @Transactional(readOnly = true)
    public DomainDto getById(UUID id) {
        return CatalogMappers.toDto(loadOrThrow(id));
    }

    public DomainDto create(DomainRequest req) {
        Domain e = new Domain();
        CatalogMappers.apply(e, req);
        Domain saved = secureDataManager.save(ENTITY_CLASS, null, new EntityMutation<>(e, WRITABLE_ATTRS));
        return CatalogMappers.toDto(saved);
    }

    public DomainDto update(UUID id, DomainRequest req) {
        Domain e = loadOrThrow(id);
        CatalogMappers.apply(e, req);
        Domain saved = secureDataManager.save(ENTITY_CLASS, id, new EntityMutation<>(e, WRITABLE_ATTRS));
        return CatalogMappers.toDto(saved);
    }

    public void delete(UUID id) {
        secureDataManager.delete(ENTITY_CLASS, id);
    }

    private Domain loadOrThrow(UUID id) {
        return secureDataManager.loadOne(ENTITY_CLASS, id)
                .orElseThrow(() -> new NotFoundException("Domain not found: " + id));
    }
}
