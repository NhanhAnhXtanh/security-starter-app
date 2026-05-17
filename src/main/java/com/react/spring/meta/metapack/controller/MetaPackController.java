package com.react.spring.meta.metapack.controller;

import com.react.spring.meta.metapack.dto.MetaPackDto;
import com.react.spring.meta.metapack.dto.MetaPackVersionDto;
import com.react.spring.meta.metapack.entity.MetaPackRegistration;
import com.react.spring.meta.metapack.service.MetaPackService;
import com.react.spring.meta.metapack.service.MetaPackRegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/metapacks")
public class MetaPackController {

    @Autowired
    private MetaPackService metaPackService;

    @Autowired
    private MetaPackRegistrationService registrationService;

    @GetMapping
    public ResponseEntity<List<MetaPackDto>> getAll() {
        return ResponseEntity.ok(metaPackService.findAll());
    }

    @GetMapping("/{identifier}")
    public ResponseEntity<MetaPackDto> getById(@PathVariable String identifier) {
        return metaPackService.findByIdOrCode(identifier)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<MetaPackDto> create(@RequestBody MetaPackDto dto) {
        return ResponseEntity.ok(metaPackService.create(dto));
    }

    @PutMapping("/{identifier}")
    public ResponseEntity<MetaPackDto> update(@PathVariable String identifier, @RequestBody MetaPackDto dto) {
        return ResponseEntity.ok(metaPackService.updateByIdentifier(identifier, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        metaPackService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<List<MetaPackVersionDto>> getVersions(@PathVariable UUID id) {
        return ResponseEntity.ok(metaPackService.listVersions(id));
    }

    // Registration endpoints
    @GetMapping("/{id}/registrations")
    public ResponseEntity<List<MetaPackRegistration>> getRegistrations(@PathVariable UUID id) {
        return ResponseEntity.ok(registrationService.findByMetaPackId(id));
    }

    @PostMapping("/{id}/registrations")
    public ResponseEntity<MetaPackRegistration> createRegistration(
            @PathVariable UUID id,
            @RequestParam String subscriberName,
            @RequestBody String requestedFields) {
        return ResponseEntity.ok(registrationService.createRegistration(id, subscriberName, requestedFields));
    }

    @PostMapping("/registrations/{regId}/approve")
    public ResponseEntity<MetaPackRegistration> approveRegistration(
            @PathVariable UUID regId,
            @RequestParam String apiSettings,
            @RequestParam(required = false) Integer customLimitPm,
            @RequestParam(required = false) Integer customLimitPd) {
        return ResponseEntity.ok(registrationService.approveRegistration(regId, apiSettings, customLimitPm, customLimitPd));
    }

    @PostMapping("/registrations/{regId}/revoke")
    public ResponseEntity<Void> revokeRegistration(@PathVariable UUID regId) {
        registrationService.revokeRegistration(regId);
        return ResponseEntity.ok().build();
    }
}
