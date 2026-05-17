package com.react.spring.catalog.service;

import com.react.spring.catalog.dto.DomainDto;
import com.react.spring.catalog.dto.DomainRequest;
import com.react.spring.catalog.entity.Domain;
import com.react.spring.common.exception.NotFoundException;
import com.react.spring.catalog.mapper.CatalogMappers;
import com.react.spring.catalog.repository.DomainRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class DomainService {

    private final DomainRepository repo;

    public DomainService(DomainRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public Page<DomainDto> list(Pageable pageable) {
        return repo.findAll(pageable).map(CatalogMappers::toDto);
    }

    @Transactional(readOnly = true)
    public DomainDto getById(UUID id) {
        return CatalogMappers.toDto(loadOrThrow(id));
    }

    public DomainDto create(DomainRequest req) {
        Domain e = new Domain();
        CatalogMappers.apply(e, req);
        return CatalogMappers.toDto(repo.save(e));
    }

    public DomainDto update(UUID id, DomainRequest req) {
        Domain e = loadOrThrow(id);
        CatalogMappers.apply(e, req);
        return CatalogMappers.toDto(repo.save(e));
    }

    public void delete(UUID id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Domain not found: " + id);
        }
        repo.deleteById(id);
    }

    private Domain loadOrThrow(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Domain not found: " + id));
    }
}
