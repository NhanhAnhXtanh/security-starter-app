package com.react.spring.meta.metasource.connect.rest.dto;

import java.util.UUID;

public class ApiFormDataFieldDTO {

    private UUID id;
    private String key;
    private String value;
    private String type = "TEXT";
    private String description;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
