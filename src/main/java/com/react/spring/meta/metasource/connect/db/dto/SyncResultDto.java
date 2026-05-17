package com.react.spring.meta.metasource.connect.db.dto;

import com.react.spring.meta.metasync.dto.MetaSyncDto;

import java.util.List;

public record SyncResultDto(
        int created,
        int skipped,
        List<MetaSyncDto> items,
        String syncMode,
        String message
) {}
