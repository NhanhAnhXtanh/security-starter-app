package com.react.spring.meta.metaset.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.react.spring.meta.metaset.entity.dto.MetaSetApiSettingConfig;

public final class MetaSetApiSettingConfigConverter {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private MetaSetApiSettingConfigConverter() {
    }

    public static MetaSetApiSettingConfig fromJson(String json) {
        if (json == null || json.isBlank()) {
            return new MetaSetApiSettingConfig();
        }
        try {
            return MAPPER.readValue(json, MetaSetApiSettingConfig.class);
        } catch (Exception ignored) {
            return new MetaSetApiSettingConfig();
        }
    }

    public static String toJson(MetaSetApiSettingConfig config) {
        try {
            return MAPPER.writeValueAsString(config == null ? new MetaSetApiSettingConfig() : config);
        } catch (Exception e) {
            throw new IllegalArgumentException("Khong the chuyen doi api setting config sang JSON", e);
        }
    }

}
