package com.react.spring.meta.metasource.connect.db.dto;

import java.util.List;
import java.util.Map;

public record QueryResultDto(
        List<String> columns,
        List<Map<String, Object>> rows,
        int count,
        long latencyMs
) {}
