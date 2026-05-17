package com.react.spring.catalog.service;

import com.react.spring.catalog.dto.TagDto;
import com.react.spring.catalog.dto.TagRequest;
import com.react.spring.catalog.entity.Tag;
import com.react.spring.common.exception.NotFoundException;
import com.react.spring.catalog.mapper.CatalogMappers;
import com.react.spring.catalog.repository.TagRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class TagService {

    private final TagRepository repo;

    public TagService(TagRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public Page<TagDto> list(Pageable pageable) {
        return repo.findAll(pageable).map(CatalogMappers::toDto);
    }

    @Transactional(readOnly = true)
    public TagDto getById(UUID id) {
        return CatalogMappers.toDto(loadOrThrow(id));
    }

    public TagDto create(TagRequest req) {
        Tag e = new Tag();
        CatalogMappers.apply(e, req);
        return CatalogMappers.toDto(repo.save(e));
    }

    public TagDto update(UUID id, TagRequest req) {
        Tag e = loadOrThrow(id);
        CatalogMappers.apply(e, req);
        return CatalogMappers.toDto(repo.save(e));
    }

    public void delete(UUID id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Tag not found: " + id);
        }
        repo.deleteById(id);
    }

    private Tag loadOrThrow(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Tag not found: " + id));
    }
}
