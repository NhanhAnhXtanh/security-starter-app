package com.react.spring.meta.metasync.service;

import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chạy một lần khi app khởi động: set is_active = true cho các record cũ có is_active = null
 * (tạo trước khi cột is_active được thêm vào schema).
 *
 * Per rules/data-access.md §2.1: ApplicationRunner bootstrap migration —
 * no user context, EntityManager direct use is allowed.
 */
@Component
public class MetaSyncStartupMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MetaSyncStartupMigration.class);

    private final EntityManager entityManager;

    public MetaSyncStartupMigration(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Bước 1: strip "-v1" suffix khỏi code của version 1
        int stripped = entityManager.createNativeQuery(
                "UPDATE core_meta_sync SET code = regexp_replace(code, '-v1$', '') " +
                "WHERE version_no = 1 AND code ~ '-v[0-9]+$'")
            .executeUpdate();
        if (stripped > 0) {
            log.info("[MetaSync Migration] Stripped version suffix from {} v1 code(s)", stripped);
        }
        // Bước 2: normalize code thành mã MetaSync dạng số theo từng MetaSource.
        int normalizedCodes = entityManager.createNativeQuery(
                """
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
                """)
            .executeUpdate();
        if (normalizedCodes > 0) {
            log.info("[MetaSync Migration] Normalized {} code(s) to MetaSync source code", normalizedCodes);
        }
        // Bước 3: reset tất cả về false
        entityManager.createQuery("UPDATE MetaSync ms SET ms.active = false").executeUpdate();
        // Bước 4: set true cho version cao nhất của mỗi nhóm
        int activated = entityManager.createNativeQuery(
                """
                UPDATE core_meta_sync ms
                SET is_active = true
                WHERE ms.version_no = (
                    SELECT MAX(ms2.version_no)
                    FROM core_meta_sync ms2
                    WHERE ms2.data_source_id IS NOT DISTINCT FROM ms.data_source_id
                      AND ms2.meta_code IS NOT DISTINCT FROM ms.meta_code
                )
                """)
            .executeUpdate();
        entityManager.clear();
        log.info("[MetaSync Migration] Recomputed is_active: {} record(s) set to active", activated);
    }
}
