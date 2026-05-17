package com.react.spring.meta.metaset.repository;

import com.react.spring.meta.metaset.entity.MetaSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Repository
public interface MetaSetRepository extends JpaRepository<MetaSet, UUID>, JpaSpecificationExecutor<MetaSet> {

    Optional<MetaSet> findByCode(String code);

    Optional<MetaSet> findFirstByMetaSourceIdAndMetaCode(UUID metaSourceId, String metaCode);

    boolean existsByCode(String code);

    @Query(value = "SELECT COALESCE(MAX(CAST(code AS INTEGER)), 0) FROM core_meta_set WHERE code ~ '^[0-9]+$'", nativeQuery = true)
    int findMaxNumericCode();


    List<MetaSet> findAllByMetaSourceId(UUID metaSourceId);

    @Query("""
            SELECT DISTINCT ms
            FROM MetaSet ms
            LEFT JOIN ms.currentVersion currentVersion
            WHERE currentVersion.metasyncCode = :metasyncCode
               OR EXISTS (
                   SELECT 1
                   FROM MetaSetVersion version
                    WHERE version.metaCode = ms.metaCode
                     AND version.metasyncCode = :metasyncCode
               )
            ORDER BY ms.name ASC
            """)
    List<MetaSet> findAllByMetasyncCode(@Param("metasyncCode") String metasyncCode);

    // Delete all MetaSets that were extracted from a MetaSync
    @Modifying(clearAutomatically = true)
    @Query("""
            DELETE FROM MetaSet ms
            WHERE ms.id IN (
                SELECT DISTINCT ms2.id
                FROM MetaSet ms2
                LEFT JOIN ms2.currentVersion currentVersion
                WHERE currentVersion.metasyncCode = :metasyncCode
                   OR EXISTS (
                       SELECT 1
                       FROM MetaSetVersion version
                        WHERE version.metaCode = ms2.metaCode
                          AND version.metasyncCode = :metasyncCode
                   )
            )
            """)
    int deleteAllByMetasyncCode(@Param("metasyncCode") String metasyncCode);
}
