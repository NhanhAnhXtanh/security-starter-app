package com.react.spring.catalog.mapper;

import com.react.spring.catalog.dto.DomainDto;
import com.react.spring.catalog.dto.DomainRequest;
import com.react.spring.catalog.dto.OrganizationDto;
import com.react.spring.catalog.dto.OrganizationRequest;
import com.react.spring.catalog.dto.TagDto;
import com.react.spring.catalog.dto.TagRequest;
import com.react.spring.catalog.entity.Domain;
import com.react.spring.catalog.entity.Organization;
import com.react.spring.catalog.entity.Tag;

public final class CatalogMappers {

    private CatalogMappers() {}

    public static OrganizationDto toDto(Organization e) {
        return new OrganizationDto(
                e.getId(), e.getName(), e.getDescription(),
                e.getCreatedDate(), e.getLastModifiedDate()
        );
    }

    public static void apply(Organization e, OrganizationRequest req) {
        e.setName(req.name());
        e.setDescription(req.description());
    }

    public static DomainDto toDto(Domain e) {
        return new DomainDto(
                e.getId(), e.getName(), e.getDescription(),
                e.getCreatedDate(), e.getLastModifiedDate()
        );
    }

    public static void apply(Domain e, DomainRequest req) {
        e.setName(req.name());
        e.setDescription(req.description());
    }

    public static TagDto toDto(Tag e) {
        return new TagDto(
                e.getId(), e.getName(), e.getDescription(),
                e.getCreatedDate(), e.getLastModifiedDate()
        );
    }

    public static void apply(Tag e, TagRequest req) {
        e.setName(req.name());
        e.setDescription(req.description());
    }
}
