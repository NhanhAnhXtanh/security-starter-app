package com.react.spring.meta.metasync.service;

import com.react.spring.meta.metasource.connect.db.dto.SyncResultDto;
import com.react.spring.meta.metasource.entity.MetaSource;
import com.vn.security.core.security.data.UnconstrainedDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MetaSyncPollingService {

    private static final Logger log = LoggerFactory.getLogger(MetaSyncPollingService.class);

    private final MetaSyncService metaSyncService;
    // Scheduled job — no user context, system bootstrap path per rules §2.1.
    private final UnconstrainedDataManager unconstrainedDataManager;

    @Value("${metasync.polling.enabled:true}")
    private boolean pollingEnabled;

    public MetaSyncPollingService(MetaSyncService metaSyncService,
                                  UnconstrainedDataManager unconstrainedDataManager) {
        this.metaSyncService = metaSyncService;
        this.unconstrainedDataManager = unconstrainedDataManager;
    }

    @Scheduled(cron = "${metasync.polling.cron:0 */5 * * * *}")
    public void pollAllSources() {
        if (!pollingEnabled) {
            return;
        }

        List<MetaSource> sources = unconstrainedDataManager.loadListByJpql(
                MetaSource.class,
                "select s from MetaSource s where s.enabled = true",
                Map.of(), null);
        if (sources.isEmpty()) {
            return;
        }

        log.info("[MetaSync Polling] Starting poll for {} source(s)", sources.size());

        int totalCreated = 0;
        int totalSkipped = 0;
        int failed = 0;

        for (MetaSource source : sources) {
            try {
                SyncResultDto result = metaSyncService.initSync(source.getId());
                totalCreated += result.created();
                totalSkipped += result.skipped();
                log.debug("[MetaSync Polling] source={} created={} skipped={}",
                        source.getCode(), result.created(), result.skipped());
            } catch (Exception ex) {
                failed++;
                log.warn("[MetaSync Polling] source={} failed: {}", source.getCode(), ex.getMessage());
            }
        }

        log.info("[MetaSync Polling] Done — created={} skipped={} failed={}",
                totalCreated, totalSkipped, failed);
    }
}
