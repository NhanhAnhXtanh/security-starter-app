package com.react.spring.meta.metapack.dto;

import com.react.spring.meta.metapack.entity.MetaPack;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MetaPackMapper {
    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    
    public MetaPackDto toDto(MetaPack entity) {
        if (entity == null) return null;
        MetaPackDto dto = new MetaPackDto();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setStatus(entity.getStatus());
        dto.setMaxRequestsPerMinute(entity.getMaxRequestsPerMinute());
        dto.setMaxRequestsPerDay(entity.getMaxRequestsPerDay());
        if (entity.getCurrentVersion() != null) {
            dto.setCurrentVersionId(entity.getCurrentVersion().getId());
            if (entity.getCurrentVersion().getDataConfig() != null) {
                try {
                    List<MetaPackVersionItemDto> items = objectMapper.readValue(
                        entity.getCurrentVersion().getDataConfig(), 
                        new TypeReference<List<MetaPackVersionItemDto>>() {}
                    );
                    dto.setVersionItems(items);
                } catch (Exception e) {
                    dto.setVersionItems(new ArrayList<>());
                }
            }
        }
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedDate());
        dto.setUpdatedBy(entity.getLastModifiedBy());
        dto.setUpdatedAt(entity.getLastModifiedDate());
        return dto;
    }
    public void updateEntityFromDto(MetaPackDto dto, MetaPack entity) {
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        if (dto.getMaxRequestsPerMinute() != null) entity.setMaxRequestsPerMinute(dto.getMaxRequestsPerMinute());
        if (dto.getMaxRequestsPerDay() != null) entity.setMaxRequestsPerDay(dto.getMaxRequestsPerDay());
    }
}
