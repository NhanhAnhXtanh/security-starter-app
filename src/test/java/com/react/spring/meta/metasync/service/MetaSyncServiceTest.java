package com.react.spring.meta.metasync.service;

import com.react.spring.common.enums.ConnectorType;
import com.react.spring.common.enums.SourceType;
import com.react.spring.meta.metasource.connect.db.MetaSourceConnectionService;
import com.react.spring.meta.metasource.connect.db.dto.SchemaDto;
import com.react.spring.meta.metasource.entity.MetaSource;
import com.vn.security.core.security.data.SecureDataManager;
import com.vn.security.core.security.data.UnconstrainedDataManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetaSyncServiceTest {

    private final SecureDataManager secureDataManager = mock(SecureDataManager.class);
    private final UnconstrainedDataManager unconstrainedDataManager = mock(UnconstrainedDataManager.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final MetaSourceConnectionService connectionService = mock(MetaSourceConnectionService.class);

    private final MetaSyncService service = new MetaSyncService(
            secureDataManager,
            unconstrainedDataManager,
            entityManager,
            connectionService);

    @Test
    void initSyncReadsThroughSystemDataPath() {
        UUID sourceId = UUID.randomUUID();
        MetaSource source = new MetaSource();
        source.setId(sourceId);
        source.setCode("src");
        source.setName("Source");
        source.setSourceType(SourceType.DATABASE);
        source.setConnectorType(ConnectorType.POSTGRES);

        Query numericCodeQuery = mock(Query.class);
        when(entityManager.createNativeQuery(contains("SELECT ms.code"))).thenReturn(numericCodeQuery);
        when(numericCodeQuery.setParameter("metaSourceId", sourceId)).thenReturn(numericCodeQuery);
        when(numericCodeQuery.getResultList()).thenReturn(List.of("00001"));

        when(unconstrainedDataManager.loadListByJpql(
                eq(MetaSource.class),
                anyString(),
                anyMap(),
                isNull()))
                .thenReturn(List.of(source));
        when(unconstrainedDataManager.loadListByJpql(
                eq(com.react.spring.meta.metasync.entity.MetaSync.class),
                anyString(),
                anyMap(),
                isNull()))
                .thenReturn(List.of());
        when(connectionService.fetchSchema(sourceId)).thenReturn(new SchemaDto(List.of()));

        var result = service.initSync(sourceId);

        assertEquals(0, result.created());
        assertEquals(0, result.skipped());
        verify(unconstrainedDataManager).loadListByJpql(eq(MetaSource.class), anyString(), anyMap(), isNull());
        verify(unconstrainedDataManager).loadListByJpql(
                eq(com.react.spring.meta.metasync.entity.MetaSync.class),
                anyString(),
                anyMap(),
                isNull());
        verifyNoInteractions(secureDataManager);
    }
}
