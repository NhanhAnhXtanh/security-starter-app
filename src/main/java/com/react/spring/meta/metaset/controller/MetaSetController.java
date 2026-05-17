package com.react.spring.meta.metaset.controller;

import com.react.spring.meta.metaset.dto.MetaSetActionRequest;
import com.react.spring.meta.metaset.dto.MetaSetDto;
import com.react.spring.meta.metaset.dto.MetaSetRequest;
import com.react.spring.meta.metaset.service.MetaSetService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/meta-sets")
public class MetaSetController {

    private final MetaSetService service;

    public MetaSetController(MetaSetService service) {
        this.service = service;
    }

    @GetMapping
    public Page<MetaSetDto> list(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String keyword,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String metaCode,
            @org.springframework.web.bind.annotation.RequestParam(required = false) java.util.UUID organizationId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) java.util.UUID domainId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) java.util.UUID metaSourceId,
            Pageable pageable) {
        return service.list(keyword, metaCode, organizationId, domainId, metaSourceId, pageable);
    }

    @GetMapping("/by-source/{metaSourceId}")
    public List<MetaSetDto> listBySource(@PathVariable UUID metaSourceId) {
        return service.listByMetaSource(metaSourceId);
    }

    @GetMapping("/by-metasync-code/{metasyncCode}")
    public List<MetaSetDto> listByMetasyncCode(@PathVariable String metasyncCode) {
        return service.listByMetasyncCode(metasyncCode);
    }

    @GetMapping("/{id}")
    public MetaSetDto get(@PathVariable UUID id) {
        return service.getById(id);
    }

    @GetMapping("/by-code/{code}")
    public MetaSetDto getByCode(@PathVariable String code) {
        return service.getByCode(code);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MetaSetDto create(@Valid @RequestBody MetaSetRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public MetaSetDto update(@PathVariable UUID id, @Valid @RequestBody MetaSetRequest req) {
        return service.update(id, req);
    }

    @PostMapping("/{id}/publish")
    public MetaSetDto publish(@PathVariable UUID id, @Valid @RequestBody MetaSetActionRequest req) {
        return service.publish(id, req);
    }

    @PostMapping("/{id}/discontinue")
    public MetaSetDto discontinue(@PathVariable UUID id, @Valid @RequestBody MetaSetActionRequest req) {
        return service.discontinue(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
