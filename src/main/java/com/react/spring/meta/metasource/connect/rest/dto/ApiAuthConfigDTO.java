package com.react.spring.meta.metasource.connect.rest.dto;

import com.react.spring.common.enums.ApiAuthType;
import com.react.spring.common.enums.ApiKeyPlacement;

import java.util.UUID;

public class ApiAuthConfigDTO {

    private UUID id;
    private ApiAuthType authType = ApiAuthType.NONE;
    private String username;
    private String password;
    private String bearerToken;
    private String apiKeyName;
    private String apiKeyValue;
    private ApiKeyPlacement apiKeyPlacement;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public ApiAuthType getAuthType() { return authType; }
    public void setAuthType(ApiAuthType authType) { this.authType = authType; }

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

    public ApiKeyPlacement getApiKeyPlacement() { return apiKeyPlacement; }
    public void setApiKeyPlacement(ApiKeyPlacement apiKeyPlacement) { this.apiKeyPlacement = apiKeyPlacement; }
}
