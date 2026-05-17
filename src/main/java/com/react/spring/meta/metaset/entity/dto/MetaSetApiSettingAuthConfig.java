package com.react.spring.meta.metaset.entity.dto;

public class MetaSetApiSettingAuthConfig {

    private String authType;
    private String username;
    private String password;
    private String bearerToken;
    private String apiKeyName;
    private String apiKeyValue;
    private String apiKeyPlacement;

    public String getAuthType() { return authType; }
    public void setAuthType(String authType) { this.authType = authType; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getBearerToken() { return bearerToken; }
    public void setBearerToken(String bearerToken) { this.bearerToken = bearerToken; }

    public String getApiKeyName() { return apiKeyName; }
    public void setApiKeyName(String apiKeyName) { this.apiKeyName = apiKeyName; }

    public String getApiKeyValue() { return apiKeyValue; }
    public void setApiKeyValue(String apiKeyValue) { this.apiKeyValue = apiKeyValue; }

    public String getApiKeyPlacement() { return apiKeyPlacement; }
    public void setApiKeyPlacement(String apiKeyPlacement) { this.apiKeyPlacement = apiKeyPlacement; }
}
