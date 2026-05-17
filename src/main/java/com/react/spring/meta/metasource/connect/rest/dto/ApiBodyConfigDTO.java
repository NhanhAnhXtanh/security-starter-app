package com.react.spring.meta.metasource.connect.rest.dto;

import com.react.spring.common.enums.ApiBodyType;

import java.util.List;
import java.util.UUID;

public class ApiBodyConfigDTO {

    private UUID id;
    private ApiBodyType bodyType = ApiBodyType.NONE;
    private String rawContent;
    private List<ApiFormDataFieldDTO> formDataFields;
    private List<ApiUrlEncodedFieldDTO> urlEncodedFields;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public ApiBodyType getBodyType() { return bodyType; }
    public void setBodyType(ApiBodyType bodyType) { this.bodyType = bodyType; }

    public String getRawContent() { return rawContent; }
    public void setRawContent(String rawContent) { this.rawContent = rawContent; }

    public List<ApiFormDataFieldDTO> getFormDataFields() { return formDataFields; }
    public void setFormDataFields(List<ApiFormDataFieldDTO> formDataFields) { this.formDataFields = formDataFields; }

    public List<ApiUrlEncodedFieldDTO> getUrlEncodedFields() { return urlEncodedFields; }
    public void setUrlEncodedFields(List<ApiUrlEncodedFieldDTO> urlEncodedFields) { this.urlEncodedFields = urlEncodedFields; }
}
