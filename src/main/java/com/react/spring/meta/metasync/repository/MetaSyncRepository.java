package com.react.spring.meta.metasync.repository;

import com.react.spring.meta.metasync.entity.MetaSync;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MetaSyncRepository extends JpaRepository<MetaSync, UUID> {

    Optional<MetaSync> findFirstByCodeAndActiveTrueOrderByMetaCodeAscVersionNoDesc(String code);

    boolean existsByCode(String code);

    @Query(value = """
            SELECT ms.code
            FROM core_meta_sync ms
            WHERE ms.data_source_id = :metaSourceId
              AND ms.code ~ '^[0-9]+$'
            ORDER BY ms.created_date ASC NULLS LAST, ms.code ASC
            LIMIT 1
            """, nativeQuery = true)
    Optional<String> findFirstNumericCodeByMetaSourceId(@Param("metaSourceId") UUID metaSourceId);

    @Query(value = "SELECT COALESCE(MAX(CAST(code AS INTEGER)), 0) FROM core_meta_sync WHERE code ~ '^[0-9]+$'", nativeQuery = true)
    int findMaxNumericCode();


    // Tìm version mới nhất theo versionNo, không quan tâm active (dùng trong initSync để so sánh hash)
    Optional<MetaSync> findFirstByMetaSourceIdAndMetaCodeOrderByVersionNoDesc(UUID dataSourceId, String metaCode);

    // Tìm version active hiện tại (dùng để hiển thị)
    Optional<MetaSync> findFirstByMetaSourceIdAndMetaCodeAndActiveTrueOrderByVersionNoDesc(UUID dataSourceId, String metaCode);

    // Tìm tất cả latest per metaCode cho 1 source (để detect bảng bị xóa)
    @Query("SELECT ms FROM MetaSync ms WHERE ms.metaSource.id = :metaSourceId " +
           "AND ms.versionNo = (SELECT MAX(ms2.versionNo) FROM MetaSync ms2 " +
           "                    WHERE ms2.metaSource.id = :metaSourceId AND ms2.metaCode = ms.metaCode)")
    List<MetaSync> findLatestPerMetaCode(@Param("metaSourceId") UUID metaSourceId);

    // Deactivate tất cả records của (source, metaCode) — kể cả null — trước khi promote version mới
    @Modifying(clearAutomatically = true)
    @Query("UPDATE MetaSync ms SET ms.active = false " +
           "WHERE ms.metaSource.id = :metaSourceId AND ms.metaCode = :metaCode")
    int deactivateAll(@Param("metaSourceId") UUID metaSourceId, @Param("metaCode") String metaCode);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE MetaSync ms SET ms.active = false " +
           "WHERE ms.metaSource.id = :metaSourceId AND ms.metaCode = :metaCode AND ms.id <> :id")
    int deactivateAllExcept(@Param("metaSourceId") UUID metaSourceId,
                            @Param("metaCode") String metaCode,
                            @Param("id") UUID id);

    // Data migration: reset toàn bộ is_active về false trước khi re-compute
    @Modifying(clearAutomatically = true)
    @Query("UPDATE MetaSync ms SET ms.active = false")
    int resetAllActive();

    // Data migration: strip suffix "-v1" khỏi code của các version 1 (ví dụ "user-v1" → "user")
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE core_meta_sync SET code = regexp_replace(code, '-v1$', '') " +
                   "WHERE version_no = 1 AND code ~ '-v[0-9]+$'", nativeQuery = true)
    int stripVersionSuffixFromV1Codes();

    // Data migration: MetaSync.code is a numeric group code per MetaSource, not per-table code.
    @Modifying(clearAutomatically = true)
    @Query(value = """
            WITH source_codes AS (
                SELECT
                    data_source_id,
                    LPAD(ROW_NUMBER() OVER (
                        ORDER BY MIN(created_date) NULLS LAST, data_source_id
                    )::TEXT, 6, '0') AS sync_code
                FROM core_meta_sync
                WHERE data_source_id IS NOT NULL
                GROUP BY data_source_id
            )
            UPDATE core_meta_sync ms
            SET code = source_codes.sync_code
            FROM source_codes
            WHERE ms.data_source_id = source_codes.data_source_id
              AND ms.code IS DISTINCT FROM source_codes.sync_code
            """, nativeQuery = true)
    int normalizeCodesToMetaSyncSourceCode();

    // Data migration: set is_active=true cho version cao nhất của mỗi (source, metaCode)
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE core_meta_sync ms
            SET is_active = true
            WHERE ms.version_no = (
                SELECT MAX(ms2.version_no)
                FROM core_meta_sync ms2
                WHERE ms2.data_source_id IS NOT DISTINCT FROM ms.data_source_id
                  AND ms2.meta_code IS NOT DISTINCT FROM ms.meta_code
            )
            """, nativeQuery = true)
    int activateLatestPerGroup();

    List<MetaSync> findByMetaSourceIdAndActiveTrueOrderByMetaCodeAsc(UUID metaSourceId);

    // Find active MetaSync excluding deleted ones (for extraction to MetaSet)
    @Query("SELECT ms FROM MetaSync ms " +
           "WHERE ms.metaSource.id = :metaSourceId " +
           "AND ms.active = true " +
           "AND ms.deleted = false " +
           "ORDER BY ms.metaCode ASC")
    List<MetaSync> findByMetaSourceIdActiveTrueNotDeletedOrderByMetaCodeAsc(@Param("metaSourceId") UUID metaSourceId);

    long countByMetaSourceId(UUID metaSourceId);

    Page<MetaSync> findByMetaSourceId(UUID metaSourceId, Pageable pageable);

    @Query(value =
           "SELECT ms FROM MetaSync ms LEFT JOIN ms.metaSource src " +
           "WHERE (:dataSourceId IS NULL OR src.id = :dataSourceId) " +
           "AND (:organizationId IS NULL OR src.organization.id = :organizationId) " +
           "AND (:domainId IS NULL OR src.domain.id = :domainId) " +
           "AND ms.active = true",
           countQuery =
           "SELECT COUNT(ms) FROM MetaSync ms LEFT JOIN ms.metaSource src " +
           "WHERE (:dataSourceId IS NULL OR src.id = :dataSourceId) " +
           "AND (:organizationId IS NULL OR src.organization.id = :organizationId) " +
           "AND (:domainId IS NULL OR src.domain.id = :domainId) " +
           "AND ms.active = true")
    Page<MetaSync> findWithFilters(
            @Param("dataSourceId") UUID dataSourceId,
            @Param("organizationId") UUID organizationId,
            @Param("domainId") UUID domainId,
            Pageable pageable);

    @Query(value =
           "SELECT ms FROM MetaSync ms LEFT JOIN ms.metaSource src " +
           "WHERE (:dataSourceId IS NULL OR src.id = :dataSourceId) " +
           "AND (:organizationId IS NULL OR src.organization.id = :organizationId) " +
           "AND (:domainId IS NULL OR src.domain.id = :domainId) " +
           "AND ms.active = true " +
           "AND (LOWER(ms.metaName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(ms.code) LIKE LOWER(CONCAT('%', :keyword, '%')))",
           countQuery =
           "SELECT COUNT(ms) FROM MetaSync ms LEFT JOIN ms.metaSource src " +
           "WHERE (:dataSourceId IS NULL OR src.id = :dataSourceId) " +
           "AND (:organizationId IS NULL OR src.organization.id = :organizationId) " +
           "AND (:domainId IS NULL OR src.domain.id = :domainId) " +
           "AND ms.active = true " +
           "AND (LOWER(ms.metaName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(ms.code) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<MetaSync> findWithFiltersAndKeyword(
            @Param("dataSourceId") UUID dataSourceId,
            @Param("organizationId") UUID organizationId,
            @Param("domainId") UUID domainId,
            @Param("keyword") String keyword,
            Pageable pageable);
}
