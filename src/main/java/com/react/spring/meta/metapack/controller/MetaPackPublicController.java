package com.react.spring.meta.metapack.controller;

import com.react.spring.meta.metapack.entity.MetaPackRegistration;
import com.react.spring.meta.metapack.entity.MetaPackVersion;
import com.react.spring.meta.metapack.dto.MetaPackVersionItemDto;
import com.react.spring.meta.metapack.repository.MetaPackRegistrationRepository;
import com.react.spring.meta.metapack.repository.MetaPackVersionRepository;
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

@RestController
@RequestMapping("/api/v1/public/pack")
public class MetaPackPublicController {

    @Autowired
    private MetaPackRegistrationRepository registrationRepository;

    @Autowired
    private MetaPackVersionRepository versionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @GetMapping("/{packCode}/data/{alias}")
    public ResponseEntity<Object> getPackData(
            @PathVariable String packCode,
            @PathVariable String alias,
            @RequestHeader(value = "X-API-Key", required = false) String apiKeyHeader,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        // 1. Extract API Key
        String apiKey = apiKeyHeader;
        if (apiKey == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            apiKey = authHeader.substring(7);
        }

        if (apiKey == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Missing API Key"));
        }

        // 2. Validate Registration
        Optional<MetaPackRegistration> regOpt = registrationRepository.findByApiKey(apiKey);
        if (regOpt.isEmpty() || !"APPROVED".equals(regOpt.get().getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid or inactive API Key"));
        }

        MetaPackRegistration reg = regOpt.get();

        // 3. Check Pack Code matches
        if (!reg.getMetaPack().getCode().equals(packCode)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "API Key not valid for this MetaPack"));
        }

        // 4. Rate Limiting (Placeholder for actual rate limiting logic)
        // Here we would use Redis or an in-memory counter using reg.getCustomRateLimitPerMinute()

        // 5. Find current version and alias
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
                // Simple search at top level for now
                matchedItem = items.stream()
                    .filter(item -> item.getEndpointAlias().equals(alias))
                    .findFirst()
                    .orElse(null);
            } catch (Exception e) {
                // Ignore
            }
        }

        if (matchedItem == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Endpoint alias not found in this MetaPack version"));
        }

        // 6. Data Fetching & Field Filtering
        // In a real implementation, we would query the actual database based on matchedItem.getMetaSetVersion()
        // and filter the columns based on reg.getRequestedFields().
        
        List<Map<String, Object>> data = List.of(
            Map.of("id", 1, "mockField", "mockValue1"),
            Map.of("id", 2, "mockField", "mockValue2")
        );

        Map<String, Object> mockData = new LinkedHashMap<>();
        mockData.put("status", "success");
        mockData.put("message", "Data fetched successfully using API Key");
        mockData.put("alias", alias);
        mockData.put("allowedFields", reg.getRequestedFields()); // Show the JSON of fields they are allowed to see
        mockData.put("data", data);

        return ResponseEntity.ok(mockData);
    }
}
