package com.react.spring.meta.metasource.connect.rest.dto;

import java.util.UUID;

public class ApiHeaderDTO {

    private UUID id;
    private String key;
    private String value;
    private Boolean required;
    private Boolean canEdit;
    private String description;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public Boolean getRequired() { return required; }
    public void setRequired(Boolean required) { this.required = required; }

    public Boolean getCanEdit() { return canEdit; }
    public void setCanEdit(Boolean canEdit) { this.canEdit = canEdit; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
