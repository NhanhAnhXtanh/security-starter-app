package com.react.spring.meta.metasource.controller;

import com.react.spring.meta.metaset.dto.MetaSetDto;
import com.react.spring.meta.metaset.service.MetaSetService;
import com.react.spring.meta.metasource.dto.MetaSourceDto;
import com.react.spring.meta.metasource.dto.MetaSourceRequest;
import com.react.spring.meta.metasync.dto.MetaSyncDto;
import com.react.spring.meta.metasync.dto.MetaSyncExtractRequest;
import com.react.spring.meta.metasource.connect.db.dto.QueryRequest;
import com.react.spring.meta.metasource.connect.db.dto.QueryResultDto;
import com.react.spring.meta.metasource.connect.rest.dto.RestProxyRequest;
import com.react.spring.meta.metasource.connect.rest.dto.RestProxyResultDto;
import com.react.spring.meta.metasource.connect.db.dto.SchemaDto;
import com.react.spring.meta.metasource.connect.db.dto.SyncResultDto;
import com.react.spring.meta.metasource.service.MetaSourceService;
import com.react.spring.meta.metasync.service.MetaSyncService;
import com.react.spring.meta.metasource.connect.db.MetaSourceConnectionService;
import com.react.spring.meta.metasource.connect.rest.MetaSourceRestService;
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
@RequestMapping("/api/meta-sources")
public class MetaSourceController {

    private final MetaSourceService service;
    private final MetaSourceConnectionService connectionService;
    private final MetaSyncService metaSyncService;
    private final MetaSourceRestService restService;
    private final MetaSetService metaSetService;

    public MetaSourceController(
            MetaSourceService service,
            MetaSourceConnectionService connectionService,
            MetaSyncService metaSyncService,
            MetaSourceRestService restService,
            MetaSetService metaSetService
    ) {
        this.service = service;
        this.connectionService = connectionService;
        this.metaSyncService = metaSyncService;
        this.restService = restService;
        this.metaSetService = metaSetService;
    }

    @GetMapping
    public Page<MetaSourceDto> list(Pageable pageable) {
        return service.list(pageable);
    }

    @GetMapping("/{id}")
    public MetaSourceDto get(@PathVariable UUID id) {
        return service.getById(id);
    }

    @GetMapping("/by-code/{code}")
    public MetaSourceDto getByCode(@PathVariable String code) {
        return service.getByCode(code);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MetaSourceDto create(@Valid @RequestBody MetaSourceRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public MetaSourceDto update(@PathVariable UUID id, @Valid @RequestBody MetaSourceRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @GetMapping("/{id}/schema")
    public SchemaDto fetchSchema(@PathVariable UUID id) {
        return connectionService.fetchSchema(id);
    }

    @PostMapping("/{id}/query")
    public QueryResultDto executeQuery(
            @PathVariable UUID id,
            @Valid @RequestBody QueryRequest req
    ) {
        return connectionService.executeQuery(id, req.sql());
    }

    @PostMapping("/{id}/sync")
    @ResponseStatus(HttpStatus.CREATED)
    public SyncResultDto initSync(@PathVariable UUID id) {
        return metaSyncService.initSync(id);
    }

    @GetMapping("/{id}/meta-syncs")
    public List<MetaSyncDto> listMetaSyncs(@PathVariable UUID id) {
        return metaSyncService.listBySource(id);
    }

    @PostMapping("/{id}/extract-to-metaset")
    @ResponseStatus(HttpStatus.CREATED)
    public List<MetaSetDto> extractToMetaSet(
            @PathVariable UUID id,
            @Valid @RequestBody MetaSyncExtractRequest req) {
        return metaSetService.extractFromMetaSource(id, req);
    }

    @PostMapping("/{id}/rest-proxy")
    public RestProxyResultDto restProxy(
            @PathVariable UUID id,
            @Valid @RequestBody RestProxyRequest req
    ) {
        return restService.proxy(id, req);
    }
}
