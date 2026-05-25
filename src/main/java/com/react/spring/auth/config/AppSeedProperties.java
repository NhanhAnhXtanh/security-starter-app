package com.react.spring.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.seed")
public record AppSeedProperties(
    boolean enabled,
    String username,
    String password,
    String email
) {}
