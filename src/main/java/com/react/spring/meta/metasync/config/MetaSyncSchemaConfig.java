package com.react.spring.meta.metasync.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class MetaSyncSchemaConfig {

    @Bean
    ApplicationRunner ensureMetaSyncVersioningSchema(JdbcTemplate jdbcTemplate) {
        return args -> {
            jdbcTemplate.execute("ALTER TABLE IF EXISTS core_meta_sync ADD COLUMN IF NOT EXISTS is_active boolean NOT NULL DEFAULT true");
            jdbcTemplate.execute("ALTER TABLE IF EXISTS core_meta_sync DROP CONSTRAINT IF EXISTS uq_core_meta_sync_source_meta_code");
        };
    }
}
