package com.react.spring.meta.metasource.connect.rest.dto;

import java.util.List;
import java.util.UUID;

public class ApiConfigDTO {

    private UUID id;
    private String method = "GET";
    private String endpointPath;
    private List<ApiHeaderDTO> headers;
    private ApiAuthConfigDTO auth;
    private ApiBodyConfigDTO body;
    private List<ApiQueryParamDTO> queryParams;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getEndpointPath() { return endpointPath; }
    public void setEndpointPath(String endpointPath) { this.endpointPath = endpointPath; }

    public List<ApiHeaderDTO> getHeaders() { return headers; }
    public void setHeaders(List<ApiHeaderDTO> headers) { this.headers = headers; }

    public ApiAuthConfigDTO getAuth() { return auth; }
    public void setAuth(ApiAuthConfigDTO auth) { this.auth = auth; }

    public ApiBodyConfigDTO getBody() { return body; }
    public void setBody(ApiBodyConfigDTO body) { this.body = body; }

    public List<ApiQueryParamDTO> getQueryParams() { return queryParams; }
    public void setQueryParams(List<ApiQueryParamDTO> queryParams) { this.queryParams = queryParams; }
}
