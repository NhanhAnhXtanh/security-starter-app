package com.react.spring.meta.metaset.entity.dto;

import java.util.ArrayList;
import java.util.List;

public class MetaSetApiSettingConfig {

    private MetaSetApiSettingAuthConfig auth;
    private List<MetaSetApiSettingHeaderConfig> headers = new ArrayList<>();
    private Integer timeoutMs;

    public MetaSetApiSettingAuthConfig getAuth() { return auth; }
    public void setAuth(MetaSetApiSettingAuthConfig auth) { this.auth = auth; }

    public List<MetaSetApiSettingHeaderConfig> getHeaders() { return headers; }
    public void setHeaders(List<MetaSetApiSettingHeaderConfig> headers) { this.headers = headers; }

    public Integer getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }
}
