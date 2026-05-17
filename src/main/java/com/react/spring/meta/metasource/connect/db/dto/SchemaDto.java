package com.react.spring.meta.metasource.connect.db.dto;

import java.util.List;

public record SchemaDto(List<TableDto> tables) {

    public record TableDto(String name, List<FieldDto> fields) {}

    public record FieldDto(
            String name,
            String type,
            Boolean nullable,
            Boolean pk,
            Fk fk
    ) {}

    public record Fk(String table, String field) {}
}
