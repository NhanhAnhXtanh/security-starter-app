package com.react.spring.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record AppJwtProperties(
    String base64Secret,
    long accessTokenTtlSeconds,
    long refreshTokenTtlSeconds
) {}
