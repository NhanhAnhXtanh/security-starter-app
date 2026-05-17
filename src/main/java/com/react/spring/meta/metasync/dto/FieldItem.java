package com.react.spring.meta.metasync.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FieldItem(
        String id,
        String code,
        String name,
        String dataType,
        String path,
        @JsonProperty("path_parent") String pathParent,
        String description,
        @JsonProperty("isNull") boolean isNull,
        @JsonProperty("isPrimaryKey") boolean isPrimaryKey,
        String comment
) {}
