package com.react.spring.meta.metasync.mapper;

import com.react.spring.meta.metasync.dto.MetaSyncDto;
import com.react.spring.meta.metasource.entity.MetaSource;
import com.react.spring.meta.metasync.entity.MetaSync;

public final class MetaSyncMapper {

    private MetaSyncMapper() {}

    public static MetaSyncDto toDto(MetaSync e) {
        MetaSource src = e.getMetaSource();
        return new MetaSyncDto(
                e.getId(),
                e.getCode(),
                e.getStatus(),
                src != null ? src.getId() : null,
                src != null ? src.getCode() : null,
                e.getMetaCode(),
                e.getMetaName(),
                e.getFieldData(),
                e.getFieldHash(),
                e.getDeleted(),
                e.getActive(),
                e.getVersionNo(),
                e.getChangedStatus(),
                e.getChangedSummary(),
                e.getCreatedDate(),
                e.getLastModifiedDate()
        );
    }
}
