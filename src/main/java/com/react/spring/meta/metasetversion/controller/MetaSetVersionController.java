package com.react.spring.meta.metasetversion.controller;

import com.react.spring.meta.metasetversion.dto.MetaSetVersionDto;
import com.react.spring.meta.metasetversion.dto.MetaSetVersionRequest;
import com.react.spring.meta.metasetversion.service.MetaSetVersionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/meta-set-versions")
public class MetaSetVersionController {

    private final MetaSetVersionService service;

    public MetaSetVersionController(MetaSetVersionService service) {
        this.service = service;
    }

    @GetMapping
    public List<MetaSetVersionDto> list(@RequestParam String metaCode) {
        return service.listByMetaCode(metaCode);
    }

    @GetMapping("/{id}")
    public MetaSetVersionDto get(@PathVariable UUID id) {
        return service.getById(id);
    }

    @GetMapping("/by-code-and-version")
    public MetaSetVersionDto getByCodeAndVersion(
            @RequestParam String metaCode,
            @RequestParam Integer versionNo
    ) {
        return service.getByMetaCodeAndVersionNo(metaCode, versionNo);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MetaSetVersionDto create(@Valid @RequestBody MetaSetVersionRequest req) {
        return service.create(req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
