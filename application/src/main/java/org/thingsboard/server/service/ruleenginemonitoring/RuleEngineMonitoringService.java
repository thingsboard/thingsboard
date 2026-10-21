/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.service.ruleenginemonitoring;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.RuleNodeInfo;
import org.thingsboard.server.common.stats.DefaultCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.service.queue.TbMsgPackProcessingContext;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.RuleEngineMonitoringDao;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.RuleNodeExecStatsRow;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.kafka.KafkaAdmin;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;

import org.thingsboard.server.dao.sql.ruleenginemonitoring.QueueLagStatsRow;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.QueueShortInfo;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.QueueStatsRow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.*;

@Slf4j
@Service
@TbRuleEngineComponent
@ConditionalOnProperty(value = "rule-node-exec-stats.enabled", havingValue = "true")
public class RuleEngineMonitoringService {
    public static final UUID NULL_RULE_NODE_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID NULL_RULE_CHAIN_UUID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final UUID NULL_QUEUE_UUID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    public static final UUID OTHERS_QUEUE_UUID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    private static final String NULL_QUEUE_NAME_KEY = " :null-queue";

    private final RuleEngineMonitoringDao dao;
    private final TbTenantProfileCache tenantProfileCache;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TopicService topicService;
    private final KafkaAdmin kafkaAdmin;
    private final long queueLagLeaderLeaseMs;
    private final QueueCacheService queueCacheService;
    private final Cache<UUID, UUID> ruleChainByRuleNodeCache;
    private final Cache<RuleNodeId, ConcurrentHashMap<String, RuleNodeExecKey>> execKeyCache;
    private final boolean queueTimeoutLastVisitedRuleNodeEnabled;
    private final int ruleNodeMaxKeys;
    private final int queueMaxKeys;
    private final AtomicReference<ConcurrentHashMap<RuleNodeExecKey, RuleNodeExecMetrics>> ruleNodeAccumulatorRef =
            new AtomicReference<>(new ConcurrentHashMap<>());
    private final AtomicReference<ConcurrentHashMap<QueueKey, QueueMetrics>> queueAccumulatorRef =
            new AtomicReference<>(new ConcurrentHashMap<>());
    private final Timer flushRuleNodeStatsTimer;
    private final Timer flushQueueStatsTimer;
    private final Timer flushQueueLagStatsTimer;
    private final Timer recordRuleNodeExecTimer;
    private final Timer recordQueueOutcomesTimer;
    private final DefaultCounter ruleNodeFlushRowsCounter;
    private final DefaultCounter queueFlushRowsCounter;
    private final DefaultCounter queueLagFlushRowsCounter;

    public RuleEngineMonitoringService(RuleEngineMonitoringDao dao,
                                       TbTenantProfileCache tenantProfileCache,
                                       TbServiceInfoProvider serviceInfoProvider,
                                       TopicService topicService,
                                       ObjectProvider<KafkaAdmin> kafkaAdminProvider,
                                       StatsFactory statsFactory,
                                       QueueCacheService queueCacheService,
                                       @Value("${rule-node-exec-stats.rule-chain-cache-ttl-min:30}") int ruleChainCacheTtlMin,
                                       @Value("${rule-node-exec-stats.exec-key-cache-ttl-min:30}") int execKeyCacheTtlMin,
                                       @Value("${rule-node-exec-stats.exec-max-keys:1000000}") int ruleNodeMaxKeys,
                                       @Value("${rule-node-exec-stats.queue-max-keys:1000000}") int queueMaxKeys,
                                       @Value("${rule-node-exec-stats.queue-timeout-last-visited-rule-node-enabled:false}") boolean queueTimeoutLastVisitedRuleNodeEnabled,
                                       @Value("${rule-node-exec-stats.queue-lag-leader-lease-ms:120000}") long queueLagLeaderLeaseMs) {
        this.dao = dao;
        this.tenantProfileCache = tenantProfileCache;
        this.serviceInfoProvider = serviceInfoProvider;
        this.topicService = topicService;
        this.kafkaAdmin = kafkaAdminProvider.getIfAvailable();
        this.queueLagLeaderLeaseMs = queueLagLeaderLeaseMs;
        this.queueTimeoutLastVisitedRuleNodeEnabled = queueTimeoutLastVisitedRuleNodeEnabled;
        this.ruleNodeMaxKeys = ruleNodeMaxKeys;
        this.queueMaxKeys = queueMaxKeys;
        this.queueCacheService = queueCacheService;
        this.ruleChainByRuleNodeCache = Caffeine.newBuilder()
                .expireAfterWrite(ruleChainCacheTtlMin, MINUTES)
                .build();
        this.execKeyCache = Caffeine.newBuilder()
                .expireAfterAccess(execKeyCacheTtlMin, MINUTES)
                .build();
        this.flushRuleNodeStatsTimer = statsFactory.createTimer("ruleNodeExecStatsFlushDuration", "statsName", "ruleNode");
        this.flushQueueStatsTimer = statsFactory.createTimer("ruleNodeExecStatsFlushDuration", "statsName", "queue");
        this.flushQueueLagStatsTimer = statsFactory.createTimer("ruleNodeExecStatsFlushDuration", "statsName", "queueLag");
        this.recordRuleNodeExecTimer = statsFactory.createTimer("ruleNodeExecStatsRecordDuration", "statsName", "ruleNode");
        this.recordQueueOutcomesTimer = statsFactory.createTimer("ruleNodeExecStatsRecordDuration", "statsName", "queue");
        this.ruleNodeFlushRowsCounter = statsFactory.createDefaultCounter("ruleNodeExecStatsFlushRows", "statsName", "ruleNode");
        this.queueFlushRowsCounter = statsFactory.createDefaultCounter("ruleNodeExecStatsFlushRows", "statsName", "queue");
        this.queueLagFlushRowsCounter = statsFactory.createDefaultCounter("ruleNodeExecStatsFlushRows", "statsName", "queueLag");
        statsFactory.createGauge("ruleNodeExecStatsAccumulatorKeys", "ruleNode", ruleNodeAccumulatorRef, ref -> ref.get().size());
        statsFactory.createGauge("ruleNodeExecStatsAccumulatorKeys", "queue", queueAccumulatorRef, ref -> ref.get().size());
        statsFactory.createGauge("ruleNodeExecStatsAccumulatorKeys", "execKey", execKeyCache, Cache::estimatedSize);
    }

    @EventListener(ComponentLifecycleMsg.class)
    public void onComponentLifecycleEvent(ComponentLifecycleMsg event) {
        switch (event.getEntityId().getEntityType()) {
            case RULE_CHAIN -> {
                switch (event.getEvent()) {
                    case CREATED, UPDATED, DELETED -> ruleChainByRuleNodeCache.invalidateAll();
                }
            }
        }
    }

    public void recordRuleNodeExec(UUID tenantId, RuleNode self, String queueName, String serviceId,
                                   long durationMs, boolean isFailure) {
        long startTs = System.currentTimeMillis();
        try {
            RuleNodeExecKey key = resolveExecKey(tenantId, self, queueName, serviceId);
            ConcurrentHashMap<RuleNodeExecKey, RuleNodeExecMetrics> accumulator = ruleNodeAccumulatorRef.get();
            RuleNodeExecMetrics metrics = accumulator.get(key);
            if (metrics == null) {
                if (accumulator.size() >= ruleNodeMaxKeys) {
                    log.debug("Rule node exec accumulator reached max keys {}, dropping key {}", ruleNodeMaxKeys, key);
                    return;
                }
                metrics = accumulator.computeIfAbsent(key, k -> new RuleNodeExecMetrics());
            }
            if (isFailure) {
                metrics.recordFailure(durationMs);
            } else {
                metrics.recordSuccess(durationMs);
            }
        } catch (Exception e) {
            log.error("Failed to record rule node exec", e);
        } finally {
            recordRuleNodeExecTimer.record(System.currentTimeMillis() - startTs, MILLISECONDS);
        }
    }

    /**
     * Records one outcome per message of a pack attempt. Successes are final; a timeout or failure is recorded only
     * when the message won't be retried ({@code toReprocess}), so a retried message counts once, with its final result.
     */
    public void recordQueueOutcomes(TbMsgPackProcessingContext packCtx, Queue queue, String serviceId, Set<UUID> toReprocess) {
        long startTs = System.currentTimeMillis();
        try {
            UUID queueTenantId = queue.getTenantId().getId();
            UUID queueId = queue.getId().getId();
            // Messages still running keep moving out of pending while this runs: copy pending first, then drop from it
            // whatever has meanwhile failed or succeeded, so no message gets two outcomes.
            Map<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> timedOut = new HashMap<>(packCtx.getPendingMap());
            Map<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> failed = new HashMap<>(packCtx.getFailedMap());
            Map<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> succeeded = new HashMap<>(packCtx.getSuccessMap());
            timedOut.keySet().removeAll(failed.keySet());
            timedOut.keySet().removeAll(succeeded.keySet());
            timedOut.keySet().removeAll(toReprocess);
            failed.keySet().removeAll(succeeded.keySet());
            failed.keySet().removeAll(toReprocess);
            recordOutcomesForMap(packCtx, timedOut, queueTenantId, queueId, serviceId, MetricType.TIMEOUT);
            recordOutcomesForMap(packCtx, failed, queueTenantId, queueId, serviceId, MetricType.FAILURE);
            recordOutcomesForMap(packCtx, succeeded, queueTenantId, queueId, serviceId, MetricType.SUCCESS);
        } catch (Exception e) {
            log.error("Failed to record queue outcomes", e);
        } finally {
            recordQueueOutcomesTimer.record(System.currentTimeMillis() - startTs, MILLISECONDS);
        }
    }

    @Scheduled(fixedDelayString = "${rule-node-exec-stats.exec-flush-interval-ms:60000}")
    public void flushRuleNodeStats() {
        ConcurrentHashMap<RuleNodeExecKey, RuleNodeExecMetrics> snapshot = ruleNodeAccumulatorRef.getAndSet(new ConcurrentHashMap<>());
        log.debug("Rule node exec snapshot has {} keys going to flush", snapshot.size());

        if (snapshot.isEmpty()) {
            return;
        }

        long bucketTime = System.currentTimeMillis();
        Map<UUID, Map<String, UUID>> queueNameToIdByTenant = buildTenantQueueMap(snapshot.keySet());
        List<RuleNodeExecStatsRow> rows = buildRuleNodeExecRows(snapshot, queueNameToIdByTenant, bucketTime);

        if (rows.isEmpty()) {
            return;
        }

        ruleNodeFlushRowsCounter.add(rows.size());
        long startTs = System.currentTimeMillis();
        try {
            dao.saveRuleNodeStats(rows);
            log.debug("Flushed {} rule node exec stats rows in {} ms", rows.size(), System.currentTimeMillis() - startTs);
        } catch (Exception e) {
            log.error("Failed to flush rule node exec stats", e);
        } finally {
            flushRuleNodeStatsTimer.record(System.currentTimeMillis() - startTs, MILLISECONDS);
        }
    }

    @Scheduled(fixedDelayString = "${rule-node-exec-stats.queue-flush-interval-ms:60000}")
    public void flushQueueStats() {
        ConcurrentHashMap<QueueKey, QueueMetrics> snapshot = queueAccumulatorRef.getAndSet(new ConcurrentHashMap<>());

        log.debug("Queue snapshot has {} keys", snapshot.size());

        if (snapshot.isEmpty()) {
            return;
        }

        Map<UUID, UUID> ruleChainByRuleNode;
        if (queueTimeoutLastVisitedRuleNodeEnabled) {
            Set<UUID> lastVisitedRuleNodeIds = snapshot.keySet().stream()
                    .filter(key -> MetricType.TIMEOUT == key.metricType())
                    .map(QueueKey::lastVisitedRuleNodeId)
                    .filter(id -> !NULL_RULE_NODE_UUID.equals(id))
                    .collect(Collectors.toSet());
            ruleChainByRuleNode = getRuleChainIds(lastVisitedRuleNodeIds);
        } else {
            ruleChainByRuleNode = Map.of();
        }

        long bucketTime = System.currentTimeMillis();
        Map<QueueRowKey, QueueRowMetrics> merged = new LinkedHashMap<>();
        for (Map.Entry<QueueKey, QueueMetrics> entry : snapshot.entrySet()) {
            QueueKey key = entry.getKey();
            long count = entry.getValue().getAndReset();
            if (count == 0) {
                continue;
            }
            UUID ruleChainId = NULL_RULE_NODE_UUID.equals(key.lastVisitedRuleNodeId())
                    ? NULL_RULE_CHAIN_UUID
                    : ruleChainByRuleNode.getOrDefault(key.lastVisitedRuleNodeId(), NULL_RULE_CHAIN_UUID);
            QueueRowKey rowKey = new QueueRowKey(key.queueTenantId(), key.tenantId(), key.queueId(),
                    ruleChainId, key.lastVisitedRuleNodeId(), key.serviceId());
            QueueRowMetrics m = merged.computeIfAbsent(rowKey, k -> new QueueRowMetrics());
            switch (key.metricType()) {
                case TIMEOUT -> m.timeoutCount += count;
                case FAILURE -> m.failureCount += count;
                case SUCCESS -> m.successCount += count;
            }
        }
        List<QueueStatsRow> rows = new ArrayList<>();
        for (Map.Entry<QueueRowKey, QueueRowMetrics> e : merged.entrySet()) {
            QueueRowKey rk = e.getKey();
            QueueRowMetrics m = e.getValue();
            rows.add(new QueueStatsRow(bucketTime, rk.queueTenantId(), rk.tenantId(), rk.queueId(),
                    rk.ruleChainId(), rk.lastVisitedRuleNodeId(), rk.serviceId(),
                    m.timeoutCount, m.failureCount, m.successCount));
        }

        if (rows.isEmpty()) {
            return;
        }

        queueFlushRowsCounter.add(rows.size());
        long startTs = System.currentTimeMillis();
        try {
            dao.saveQueueStats(rows);
            log.debug("Flushed {} queue stats rows", rows.size());
        } catch (Exception e) {
            log.error("Failed to flush queue stats", e);
        } finally {
            flushQueueStatsTimer.record(System.currentTimeMillis() - startTs, MILLISECONDS);
        }
    }

    @Scheduled(fixedDelayString = "${rule-node-exec-stats.queue-lag-flush-interval-ms:60000}")
    public void flushQueueLagStats() {
        if (kafkaAdmin == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (!dao.tryAcquireQueueLagLeader(serviceInfoProvider.getServiceId(), now, queueLagLeaderLeaseMs)) {
            log.debug("Not the queue-lag leader, skipping flush");
            return;
        }

        long startTs = System.currentTimeMillis();
        try {
            Map<QueueShortInfo, List<String>> queueToGroups = buildQueueConsumerGroups(queueCacheService.getAllQueues());
            if (queueToGroups.isEmpty()) {
                return;
            }

            Map<String, Long> lagByGroup = kafkaAdmin.getTotalLagForGroupsBulk(collectGroups(queueToGroups));
            List<QueueLagStatsRow> rows = buildLagRows(queueToGroups, lagByGroup, System.currentTimeMillis());

            queueLagFlushRowsCounter.add(rows.size());
            dao.saveQueueLagStats(rows);
            log.debug("Flushed {} queue lag stats rows", rows.size());
        } catch (Exception e) {
            log.error("Failed to flush queue lag stats", e);
        } finally {
            flushQueueLagStatsTimer.record(System.currentTimeMillis() - startTs, MILLISECONDS);
        }
    }

    private Map<QueueShortInfo, List<String>> buildQueueConsumerGroups(Collection<QueueShortInfo> queues) {
        Map<QueueShortInfo, List<String>> queueToGroups = new LinkedHashMap<>();
        for (QueueShortInfo queue : queues) {
            TenantId queueTenantId = TenantId.fromUUID(queue.tenantId());
            List<String> groups = new ArrayList<>();
            if (queue.consumerPerPartition()) {
                for (int partition = 0; partition < queue.partitions(); partition++) {
                    groups.add(topicService.buildConsumerGroupId("re-", queueTenantId, queue.name(), partition));
                }
            } else {
                groups.add(topicService.buildConsumerGroupId("re-", queueTenantId, queue.name(), null));
            }
            queueToGroups.put(queue, groups);
        }
        return queueToGroups;
    }

    private Set<String> collectGroups(Map<QueueShortInfo, List<String>> queueToGroups) {
        return queueToGroups.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet());
    }

    private List<QueueLagStatsRow> buildLagRows(Map<QueueShortInfo, List<String>> queueToGroups,
                                                Map<String, Long> lagByGroup, long bucketTime) {
        List<QueueLagStatsRow> rows = new ArrayList<>();
        for (Map.Entry<QueueShortInfo, List<String>> entry : queueToGroups.entrySet()) {
            QueueShortInfo queue = entry.getKey();
            long lag = entry.getValue().stream()
                    .mapToLong(group -> lagByGroup.getOrDefault(group, 0L))
                    .sum();
            // A 0 row is what tells "last" the backlog has drained.
            rows.add(new QueueLagStatsRow(bucketTime, queue.tenantId(), queue.id(), lag));
        }
        return rows;
    }

    private RuleNodeExecKey resolveExecKey(UUID tenantId, RuleNode self, String queueName, String serviceId) {
        ConcurrentHashMap<String, RuleNodeExecKey> perNode = execKeyCache.get(self.getId(), k -> new ConcurrentHashMap<>());
        String mapKey = queueName != null ? queueName : NULL_QUEUE_NAME_KEY;
        RuleNodeExecKey key = perNode.get(mapKey);
        if (key == null) {
            key = new RuleNodeExecKey(tenantId, null, queueName,
                    self.getRuleChainId().getId(), self.getId().getId(), serviceId);
            perNode.put(mapKey, key);
        }
        return key;
    }

    private void recordOutcomesForMap(TbMsgPackProcessingContext packCtx,
                                      Map<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> map,
                                      UUID queueTenantId, UUID queueId, String serviceId, MetricType metricType) {
        boolean isSysQueue = TenantId.SYS_TENANT_ID.getId().equals(queueTenantId);
        boolean trackLastVisitedNode = queueTimeoutLastVisitedRuleNodeEnabled && metricType == MetricType.TIMEOUT;
        if (!isSysQueue && !trackLastVisitedNode) {
            recordQueueOutcome(queueTenantId, queueTenantId, queueId, NULL_RULE_NODE_UUID, serviceId, metricType, map.size());
            return;
        }
        Map<UUID, Map<UUID, QueueMetrics>> grouped = new HashMap<>();
        for (Map.Entry<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> entry : map.entrySet()) {
            UUID msgId = entry.getKey();
            ToRuleEngineMsg proto = entry.getValue().getValue();
            UUID tenantId = isSysQueue ? new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB()) : queueTenantId;
            UUID nodeId = NULL_RULE_NODE_UUID;
            if (trackLastVisitedNode) {
                RuleNodeInfo ruleNodeInfo = packCtx.getLastVisitedRuleNode(msgId);
                if (ruleNodeInfo != null) {
                    nodeId = ruleNodeInfo.getRuleNodeId().getId();
                }
            }
            grouped.computeIfAbsent(tenantId, k -> new HashMap<>())
                    .computeIfAbsent(nodeId, k -> new QueueMetrics())
                    .increment(1);
        }
        grouped.forEach((tenantId, byNode) -> byNode.forEach((nodeId, metrics) ->
                recordQueueOutcome(queueTenantId, tenantId, queueId, nodeId, serviceId, metricType, metrics.getAndReset())));
    }

    private void recordQueueOutcome(UUID queueTenantId, UUID tenantId, UUID queueId, UUID nodeId, String serviceId, MetricType metricType, long count) {
        try {
            QueueKey key = new QueueKey(queueTenantId, tenantId, queueId, nodeId, serviceId, metricType);
            ConcurrentHashMap<QueueKey, QueueMetrics> accumulator = queueAccumulatorRef.get();
            QueueMetrics metrics = accumulator.get(key);
            if (metrics == null) {
                if (accumulator.size() >= queueMaxKeys) {
                    log.debug("Queue accumulator reached max keys {}, dropping key {}", queueMaxKeys, key);
                    return;
                }
                metrics = accumulator.computeIfAbsent(key, k -> new QueueMetrics());
            }
            metrics.increment(count);
        } catch (Exception e) {
            log.error("Failed to record queue outcome", e);
        }
    }

    private Map<UUID, Map<String, UUID>> buildTenantQueueMap(Set<RuleNodeExecKey> keys) {
        List<UUID> tenantIds = keys.stream()
                .map(RuleNodeExecKey::tenantId)
                .distinct()
                .collect(Collectors.toList());
        log.debug("Building tenant queue map for tenant ids {}", tenantIds);

        Collection<QueueShortInfo> allQueues = queueCacheService.getAllQueues();
        log.debug("Found all queues: {}", allQueues);

        Map<String, UUID> systemQueueNameToId = allQueues.stream()
                .filter(q -> TenantId.SYS_TENANT_ID.getId().equals(q.tenantId()))
                .collect(Collectors.toMap(QueueShortInfo::name, QueueShortInfo::id, (a, b) -> a));

        Map<UUID, Map<String, UUID>> result = new HashMap<>();

        for (UUID tenantId : tenantIds) {
            TenantProfile profile = tenantProfileCache.get(TenantId.fromUUID(tenantId));
            boolean usesSystemQueues = profile != null && !profile.isIsolatedTbRuleEngine();
            if (usesSystemQueues) {
                result.put(tenantId, systemQueueNameToId);
            } else {
                Map<String, UUID> tenantQueues = allQueues.stream()
                        .filter(q -> tenantId.equals(q.tenantId()))
                        .collect(Collectors.toMap(QueueShortInfo::name, QueueShortInfo::id));
                result.put(tenantId, tenantQueues);
            }
        }

        log.debug("Built tenant queue map {}", result);

        return result;
    }

    private Map<UUID, UUID> getRuleChainIds(Set<UUID> ruleNodeIds) {
        Map<UUID, UUID> result = new HashMap<>();
        Set<UUID> missing = new HashSet<>();
        for (UUID id : ruleNodeIds) {
            UUID cached = ruleChainByRuleNodeCache.getIfPresent(id);
            if (cached != null) {
                result.put(id, cached);
            } else {
                missing.add(id);
            }
        }
        if (!missing.isEmpty()) {
            Map<UUID, UUID> loaded = new HashMap<>(dao.findRuleChainIdsByRuleNodeIds(missing));
            for (UUID id : missing) {
                loaded.putIfAbsent(id, NULL_RULE_CHAIN_UUID);
            }
            ruleChainByRuleNodeCache.putAll(loaded);
            result.putAll(loaded);
        }
        return result;
    }

    private List<RuleNodeExecStatsRow> buildRuleNodeExecRows(ConcurrentHashMap<RuleNodeExecKey, RuleNodeExecMetrics> snapshot,
                                                             Map<UUID, Map<String, UUID>> queueNameToIdByTenant,
                                                             long bucketTime) {
        List<RuleNodeExecStatsRow> rows = new ArrayList<>();
        Map<RuleNodeExecKey, RuleNodeExecStatsRow> metricsFromOthersQueue = new HashMap<>();

        for (Map.Entry<RuleNodeExecKey, RuleNodeExecMetrics> entry : snapshot.entrySet()) {
            RuleNodeExecKey key = entry.getKey();
            RuleNodeExecMetrics metrics = entry.getValue();
            UUID queueId = resolveQueueId(key, queueNameToIdByTenant);

            if (OTHERS_QUEUE_UUID.equals(queueId)) {
                mergeIntoOthers(key, metrics, bucketTime, metricsFromOthersQueue);
            } else {
                rows.add(RuleNodeExecStatsRow.toRow(key.tenantId(), queueId, key.ruleChainId(), key.ruleNodeId(), key.serviceId(), bucketTime, metrics.getExecCount(), metrics.getErrorCount(), metrics.getTotalDurationMs(), metrics.getMaxDurationMs()));
            }
        }

        rows.addAll(metricsFromOthersQueue.values());
        return rows;
    }

    private UUID resolveQueueId(RuleNodeExecKey key, Map<UUID, Map<String, UUID>> queueNameToIdByTenant) {
        if (key.queueName() == null) {
            return NULL_QUEUE_UUID;
        }

        UUID queueId = queueNameToIdByTenant.getOrDefault(key.tenantId(), Map.of()).get(key.queueName());

        if (queueId == null) {
            log.debug("Queue not found for RuleNodeExecKey: {}", key);
            return OTHERS_QUEUE_UUID;
        }

        return queueId;
    }

    private void mergeIntoOthers(RuleNodeExecKey key, RuleNodeExecMetrics metrics, long bucketTime,
                                 Map<RuleNodeExecKey, RuleNodeExecStatsRow> otherQueuesMetrics) {
        RuleNodeExecKey mergeKey = new RuleNodeExecKey(key.tenantId(), OTHERS_QUEUE_UUID, null, key.ruleChainId(), key.ruleNodeId(), key.serviceId());
        RuleNodeExecStatsRow row = RuleNodeExecStatsRow.toRow(key.tenantId(), OTHERS_QUEUE_UUID, key.ruleChainId(), key.ruleNodeId(), key.serviceId(), bucketTime, metrics.getExecCount(), metrics.getErrorCount(), metrics.getTotalDurationMs(), metrics.getMaxDurationMs());
        otherQueuesMetrics.merge(mergeKey, row, (oldValue, newValue) -> new RuleNodeExecStatsRow(
                oldValue.tenantId(), OTHERS_QUEUE_UUID, oldValue.ruleChainId(), oldValue.ruleNodeId(), oldValue.serviceId(),
                bucketTime,
                oldValue.execCount() + newValue.execCount(),
                oldValue.errorCount() + newValue.errorCount(),
                oldValue.totalDurationMs() + newValue.totalDurationMs(),
                Math.max(oldValue.maxDurationMs(), newValue.maxDurationMs())
        ));
    }
}
