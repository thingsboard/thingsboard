// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ttl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.RuleEngineMonitoringDao;
import org.thingsboard.server.dao.sql.trace.TraceDao;
import org.thingsboard.server.queue.discovery.PartitionService;

import java.util.concurrent.TimeUnit;

/**
 * Expires traces and rule engine monitoring stats. Spans and stats go a whole partition at a time, so their TTLs are
 * accurate to sql.traces.partition_size and sql.traces.rule_engine_stats_partition_size.
 */
@Service
@ConditionalOnExpression("${sql.ttl.traces.enabled:true}")
@Slf4j
public class TraceCleanUpService extends AbstractCleanUpService {

    private final TraceDao traceDao;
    private final RuleEngineMonitoringDao ruleEngineMonitoringDao;

    @Value("${sql.ttl.traces.spans_ttl:604800}")
    private long spansTtlInSec;
    @Value("${sql.ttl.traces.trace_groups_ttl:604800}")
    private long traceGroupsTtlInSec;
    @Value("${sql.ttl.traces.rule_engine_stats_ttl:2592000}")
    private long ruleEngineStatsTtlInSec;
    @Value("${sql.ttl.traces.removal_batch_size:10000}")
    private int removalBatchSize;

    public TraceCleanUpService(PartitionService partitionService, TraceDao traceDao, RuleEngineMonitoringDao ruleEngineMonitoringDao) {
        super(partitionService);
        this.traceDao = traceDao;
        this.ruleEngineMonitoringDao = ruleEngineMonitoringDao;
    }

    @Scheduled(initialDelayString = "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.traces.execution_interval_ms:3600000})}",
            fixedDelayString = "${sql.ttl.traces.execution_interval_ms:3600000}")
    public void cleanUp() {
        long now = getCurrentTimeMillis();
        long spansExpTime = expTime(now, spansTtlInSec);
        long ruleEngineStatsExpTime = expTime(now, ruleEngineStatsTtlInSec);
        if (!isSystemTenantPartitionMine()) {
            // Another node drops the partitions; just forget the ones it will have dropped.
            traceDao.cleanUpPartitionsCache(spansExpTime);
            ruleEngineMonitoringDao.cleanUpPartitionsCache(ruleEngineStatsExpTime);
            return;
        }
        // Separate tries: a failure in one cleanup must not skip the other.
        try {
            log.debug("Cleaning up traces before {} and trace groups before {}", spansExpTime, expTime(now, traceGroupsTtlInSec));
            traceDao.cleanUp(spansExpTime, expTime(now, traceGroupsTtlInSec), removalBatchSize);
        } catch (Exception e) {
            log.error("Failed to clean up traces", e);
        }
        try {
            ruleEngineMonitoringDao.cleanUp(ruleEngineStatsExpTime);
        } catch (Exception e) {
            log.error("Failed to clean up rule engine stats", e);
        }
    }

    // A TTL of 0 keeps the data forever.
    private static long expTime(long now, long ttlInSec) {
        return ttlInSec > 0 ? now - TimeUnit.SECONDS.toMillis(ttlInSec) : 0;
    }

    public long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

}
