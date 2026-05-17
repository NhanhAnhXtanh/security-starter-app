package com.react.spring.meta.metasync.controller;

import com.react.spring.meta.metaset.dto.MetaSetDto;
import com.react.spring.meta.metaset.service.MetaSetService;
import com.react.spring.meta.metasync.dto.MetaSyncDto;
import com.react.spring.meta.metasync.dto.MetaSyncExtractRequest;
import com.react.spring.meta.metasync.dto.MetaSyncRequest;
import com.react.spring.meta.metasync.service.MetaSyncService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/meta-syncs")
public class MetaSyncController {

    private final MetaSyncService service;
    private final MetaSetService metaSetService;

    public MetaSyncController(MetaSyncService service, MetaSetService metaSetService) {
        this.service = service;
        this.metaSetService = metaSetService;
    }

    @GetMapping
    public Page<MetaSyncDto> list(
            @RequestParam(required = false) UUID dataSourceId,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID domainId,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        return service.list(dataSourceId, organizationId, domainId, keyword, pageable);
    }

    @GetMapping("/{id}")
    public MetaSyncDto get(@PathVariable UUID id) {
        return service.getById(id);
    }

    @GetMapping("/by-code/{code}")
    public MetaSyncDto getByCode(@PathVariable String code) {
        return service.getByCode(code);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MetaSyncDto create(@Valid @RequestBody MetaSyncRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public MetaSyncDto update(@PathVariable UUID id, @Valid @RequestBody MetaSyncRequest req) {
        return service.update(id, req);
    }

    @PostMapping("/{id}/extract-to-metaset")
    @ResponseStatus(HttpStatus.CREATED)
    public MetaSetDto extractToMetaSet(
            @PathVariable UUID id,
            @Valid @RequestBody MetaSyncExtractRequest req) {
        return metaSetService.extractFromMetaSync(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
