package com.react.spring.catalog.service;

import com.react.spring.catalog.dto.TagDto;
import com.react.spring.catalog.dto.TagRequest;
import com.react.spring.catalog.entity.Tag;
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
public class TagService {

    private static final Class<Tag> ENTITY_CLASS = Tag.class;
    private static final List<String> WRITABLE_ATTRS = List.of("name", "description");

    private final SecureDataManager secureDataManager;

    public TagService(SecureDataManager secureDataManager) {
        this.secureDataManager = secureDataManager;
    }

    @Transactional(readOnly = true)
    public Page<TagDto> list(Pageable pageable) {
        return secureDataManager.loadList(ENTITY_CLASS, pageable).map(CatalogMappers::toDto);
    }

    @Transactional(readOnly = true)
    public TagDto getById(UUID id) {
        return CatalogMappers.toDto(loadOrThrow(id));
    }

    public TagDto create(TagRequest req) {
        Tag e = new Tag();
        CatalogMappers.apply(e, req);
        Tag saved = secureDataManager.save(ENTITY_CLASS, null, new EntityMutation<>(e, WRITABLE_ATTRS));
        return CatalogMappers.toDto(saved);
    }

    public TagDto update(UUID id, TagRequest req) {
        Tag e = loadOrThrow(id);
        CatalogMappers.apply(e, req);
        Tag saved = secureDataManager.save(ENTITY_CLASS, id, new EntityMutation<>(e, WRITABLE_ATTRS));
        return CatalogMappers.toDto(saved);
    }

    public void delete(UUID id) {
        secureDataManager.delete(ENTITY_CLASS, id);
    }

    private Tag loadOrThrow(UUID id) {
        return secureDataManager.loadOne(ENTITY_CLASS, id)
                .orElseThrow(() -> new NotFoundException("Tag not found: " + id));
    }
}
