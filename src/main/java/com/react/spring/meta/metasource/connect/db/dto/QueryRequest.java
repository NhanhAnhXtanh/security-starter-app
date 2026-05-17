package com.react.spring.meta.metasource.connect.db.dto;

import jakarta.validation.constraints.NotBlank;

public record QueryRequest(@NotBlank String sql) {}
