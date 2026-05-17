package com.react.spring.meta.metapack.controller;

import com.react.spring.meta.metapack.dto.MetaPackVersionItemDto;
import com.react.spring.meta.metapack.entity.MetaPackRegistration;
import com.react.spring.meta.metapack.entity.MetaPackVersion;
import com.react.spring.meta.metapack.service.MetaPackRegistrationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Public endpoint authenticated by API key (not session JWT). Per
 * rules/data-access.md §2.3, code that already enforces its own access
 * control (apiKey lookup + APPROVED-status check below) is allowed to use
 * the by-field service lookup that bypasses SecureDataManager.
 */
@RestController
@RequestMapping("/api/v1/public/pack")
public class MetaPackPublicController {

    @Autowired
    private MetaPackRegistrationService registrationService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @GetMapping("/{packCode}/data/{alias}")
    public ResponseEntity<Object> getPackData(
            @PathVariable String packCode,
            @PathVariable String alias,
            @RequestHeader(value = "X-API-Key", required = false) String apiKeyHeader,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String apiKey = apiKeyHeader;
        if (apiKey == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            apiKey = authHeader.substring(7);
        }

        if (apiKey == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Missing API Key"));
        }

        Optional<MetaPackRegistration> regOpt = registrationService.findByApiKey(apiKey);
        if (regOpt.isEmpty() || !"APPROVED".equals(regOpt.get().getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid or inactive API Key"));
        }

        MetaPackRegistration reg = regOpt.get();

        if (!reg.getMetaPack().getCode().equals(packCode)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "API Key not valid for this MetaPack"));
        }

        if (reg.getMetaPack().getCurrentVersion() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "MetaPack has no active version"));
        }

        MetaPackVersion version = reg.getMetaPack().getCurrentVersion();

        MetaPackVersionItemDto matchedItem = null;
        if (version.getDataConfig() != null) {
            try {
                List<MetaPackVersionItemDto> items = objectMapper.readValue(
                    version.getDataConfig(),
                    new TypeReference<List<MetaPackVersionItemDto>>() {}
                );
                matchedItem = items.stream()
                    .filter(item -> item.getEndpointAlias().equals(alias))
                    .findFirst()
                    .orElse(null);
            } catch (Exception e) {
                // ignore
            }
        }

        if (matchedItem == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Endpoint alias not found in this MetaPack version"));
        }

        List<Map<String, Object>> data = List.of(
            Map.of("id", 1, "mockField", "mockValue1"),
            Map.of("id", 2, "mockField", "mockValue2")
        );

        Map<String, Object> mockData = new LinkedHashMap<>();
        mockData.put("status", "success");
        mockData.put("message", "Data fetched successfully using API Key");
        mockData.put("alias", alias);
        mockData.put("allowedFields", reg.getRequestedFields());
        mockData.put("data", data);

        return ResponseEntity.ok(mockData);
    }
}
