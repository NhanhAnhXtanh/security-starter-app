package com.react.spring.meta.metaset.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.react.spring.meta.metaset.entity.dto.MetaSetOperationConfig;

public final class MetaSetOperationConfigConverter {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private MetaSetOperationConfigConverter() {
    }

    public static MetaSetOperationConfig fromJson(String json) {
        if (json == null || json.isBlank()) {
            return new MetaSetOperationConfig();
        }
        try {
            return MAPPER.readValue(json, MetaSetOperationConfig.class);
        } catch (Exception ignored) {
            return new MetaSetOperationConfig();
        }
    }

    public static String toJson(MetaSetOperationConfig config) {
        try {
            return MAPPER.writeValueAsString(config == null ? new MetaSetOperationConfig() : config);
        } catch (Exception e) {
            throw new IllegalArgumentException("Khong the chuyen doi operation config sang JSON", e);
        }
    }

}
