package com.react.spring.meta.metasync.service;

import com.react.spring.meta.metasync.repository.MetaSyncRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chạy một lần khi app khởi động: set is_active = true cho các record cũ có is_active = null
 * (tạo trước khi cột is_active được thêm vào schema).
 */
@Component
public class MetaSyncStartupMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MetaSyncStartupMigration.class);

    private final MetaSyncRepository repo;

    public MetaSyncStartupMigration(MetaSyncRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Bước 1: strip "-v1" suffix khỏi code của version 1 (ví dụ "user-v1" → "user")
        int stripped = repo.stripVersionSuffixFromV1Codes();
        if (stripped > 0) {
            log.info("[MetaSync Migration] Stripped version suffix from {} v1 code(s)", stripped);
        }
        // Bước 2: normalize code thành mã MetaSync dạng số theo từng MetaSource.
        int normalizedCodes = repo.normalizeCodesToMetaSyncSourceCode();
        if (normalizedCodes > 0) {
            log.info("[MetaSync Migration] Normalized {} code(s) to MetaSync source code", normalizedCodes);
        }
        // Bước 3: reset tất cả về false
        repo.resetAllActive();
        // Bước 4: set true cho version cao nhất của mỗi nhóm (data_source_id, meta_code)
        int activated = repo.activateLatestPerGroup();
        log.info("[MetaSync Migration] Recomputed is_active: {} record(s) set to active", activated);
    }
}
