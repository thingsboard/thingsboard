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

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.MergedStatsDelta;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.MergedStatsTableRow;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.MetricDelta;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.QueueLagTableEntry;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.QueueLagTimeseriesEntry;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.QueueTableEntry;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.QueueTimeseriesEntry;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.RuleNodeTableRow;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.RuleNodeTsKvEntry;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.QueueLagTableQuery;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.QueueLagTimeseriesQuery;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.QueueTableQuery;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.QueueTimeseriesQuery;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.RuleEngineMonitoringDao;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.RuleEngineMonitoringFilters;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.RuleEngineMonitoringOption;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.RuleEngineMonitoringQueueOption;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.RuleEngineMonitoringRuleNodeOption;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.RuleNodeDimension;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.RuleNodeTableQuery;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.RuleNodeTimeseriesQuery;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class RuleEngineMonitoringQueryService {
    private final RuleEngineMonitoringDao dao;
    private final PartitionService partitionService;
    private final TbTenantProfileCache tenantProfileCache;

    @Value("${rule-node-exec-stats.queue-lag-flush-interval-ms:60000}")
    private long queueLagFlushIntervalMs;
    @Value("${rule-node-exec-stats.queue-lag-leader-lease-ms:120000}")
    private long queueLagLeaderLeaseMs;

    public RuleEngineMonitoringFilters getFilters(UUID tenantId) {
        List<String> serviceIds = partitionService.getAllServiceIds(ServiceType.TB_RULE_ENGINE)
                .stream().toList();

        RuleEngineMonitoringFilters filters = new RuleEngineMonitoringFilters(
                dao.findFilterQueues(tenantId),
                dao.findFilterRuleChains(tenantId),
                dao.findFilterRuleNodes(tenantId),
                serviceIds
        );

        filters.queues().add(new RuleEngineMonitoringQueueOption(RuleEngineMonitoringService.NULL_QUEUE_UUID, "No Queue", null));
        filters.queues().add(new RuleEngineMonitoringQueueOption(RuleEngineMonitoringService.OTHERS_QUEUE_UUID, "Unknown Queue", null));
        filters.ruleChains().add(new RuleEngineMonitoringOption(RuleEngineMonitoringService.NULL_RULE_CHAIN_UUID, "No Rule Chain"));
        filters.ruleNodes().add(new RuleEngineMonitoringRuleNodeOption(RuleEngineMonitoringService.NULL_RULE_NODE_UUID, "No Rule Node",
                RuleEngineMonitoringService.NULL_RULE_CHAIN_UUID, "No Rule Chain"));
        return filters;
    }

    public List<RuleNodeTsKvEntry> getNodeStatsTimeseries(
            UUID tenantId, long startTs, long endTs,
            Set<RuleNodeDimension> dims,
            List<UUID> queueIds, List<UUID> ruleChainIds, List<UUID> ruleNodeIds,
            List<String> serviceIds, Long intervalMs) {
        return dao.findRuleNodeTimeseries(
                new RuleNodeTimeseriesQuery(tenantId, startTs, endTs, dims, queueIds, ruleChainIds, ruleNodeIds, serviceIds, intervalMs));
    }

    public List<RuleNodeTableRow> getNodeStatsTable(
            UUID tenantId, long startTs, long endTs,
            Set<RuleNodeDimension> dims,
            List<UUID> queueIds, List<UUID> ruleChainIds, List<UUID> ruleNodeIds,
            List<String> serviceIds) {
        return dao.findRuleNodeTable(
                new RuleNodeTableQuery(tenantId, startTs, endTs, dims, queueIds, ruleChainIds, ruleNodeIds, serviceIds));
    }

    public List<QueueTimeseriesEntry> getQueueStatsTimeseries(
            UUID tenantId, long startTs, long endTs,
            Set<RuleNodeDimension> dims,
            List<UUID> queueIds, List<UUID> ruleChainIds, List<UUID> ruleNodeIds,
            List<String> serviceIds, Long intervalMs) {
        return dao.findQueueTimeseries(
                new QueueTimeseriesQuery(tenantId, startTs, endTs, dims, queueIds, ruleChainIds, ruleNodeIds, serviceIds, intervalMs));
    }

    public List<QueueTableEntry> getQueueStatsTable(
            UUID tenantId, long startTs, long endTs,
            Set<RuleNodeDimension> dims,
            List<UUID> queueIds, List<UUID> ruleChainIds, List<UUID> ruleNodeIds,
            List<String> serviceIds) {
        return dao.findQueueTable(
                new QueueTableQuery(tenantId, startTs, endTs, dims, queueIds, ruleChainIds, ruleNodeIds, serviceIds));
    }

    public List<QueueLagTimeseriesEntry> getQueueLagTimeseries(
            UUID tenantId, long startTs, long endTs,
            Set<RuleNodeDimension> dims,
            List<UUID> queueIds, Long intervalMs) {
        UUID queueTenantId = resolveQueueTenantId(tenantId);
        return dao.findQueueLagTimeseries(
                new QueueLagTimeseriesQuery(queueTenantId, startTs, endTs, dims, queueIds, intervalMs));
    }

    // Lag is written every flush interval by the lease holder; allow a missed flush and a leader handover.
    private long lagStaleAfterMs() {
        return 2 * queueLagFlushIntervalMs + queueLagLeaderLeaseMs;
    }

    private UUID resolveQueueTenantId(UUID tenantId) {
        return QueueOwners.resolveQueueTenantId(tenantProfileCache, tenantId);
    }

    public List<QueueLagTableEntry> getQueueLagTable(
            UUID tenantId, long startTs, long endTs,
            Set<RuleNodeDimension> dims,
            List<UUID> queueIds) {
        return dao.findQueueLagTable(
                new QueueLagTableQuery(resolveQueueTenantId(tenantId), startTs, endTs, dims, queueIds));
    }

    public long getCurrentQueueLag(UUID tenantId, List<UUID> queueIds) {
        return dao.findLastQueueLag(resolveQueueTenantId(tenantId), queueIds, lagStaleAfterMs());
    }

    public List<MergedStatsTableRow> getMergedStatsTable(
            UUID tenantId,
            long startTs, long endTs,
            Set<RuleNodeDimension> dims,
            Set<RuleNodeDimension> queueDims,
            List<UUID> queueIds,
            List<UUID> ruleChainIds,
            List<UUID> ruleNodeIds,
            List<String> serviceIds) {
        return fetchAndJoin(tenantId, startTs, endTs, dims, queueDims, queueIds, ruleChainIds, ruleNodeIds, serviceIds);
    }

    public List<MergedStatsDelta> getMergedStatsTableCompare(
            UUID tenantId,
            long baseStartTs, long baseEndTs,
            long compareStartTs, long compareEndTs,
            Set<RuleNodeDimension> dims,
            Set<RuleNodeDimension> queueDims,
            List<UUID> queueIds,
            List<UUID> ruleChainIds,
            List<UUID> ruleNodeIds,
            List<String> serviceIds) {
        List<MergedStatsTableRow> baseRows = fetchAndJoin(
                tenantId, baseStartTs, baseEndTs, dims, queueDims, queueIds, ruleChainIds, ruleNodeIds, serviceIds);
        List<MergedStatsTableRow> compareRows = fetchAndJoin(
                tenantId, compareStartTs, compareEndTs, dims, queueDims, queueIds, ruleChainIds, ruleNodeIds, serviceIds);
        return buildDeltas(baseRows, compareRows, dims);
    }

    private List<MergedStatsDelta> buildDeltas(
            List<MergedStatsTableRow> baseRows,
            List<MergedStatsTableRow> compareRows,
            Set<RuleNodeDimension> dims) {
        Map<DimKey, MergedStatsTableRow> baseMap    = index(baseRows, dims);
        Map<DimKey, MergedStatsTableRow> compareMap = index(compareRows, dims);

        java.util.LinkedHashSet<DimKey> keys = new java.util.LinkedHashSet<>(baseMap.keySet());
        keys.addAll(compareMap.keySet());

        List<MergedStatsDelta> result = new ArrayList<>();
        for (DimKey k : keys) {
            MergedStatsTableRow b = baseMap.get(k);
            MergedStatsTableRow c = compareMap.get(k);
            result.add(new MergedStatsDelta(
                    k.queueId(), k.ruleChainId(), k.ruleNodeId(), k.serviceId(),
                    metricDelta(b == null ? null : b.execCount(),       c == null ? null : c.execCount()),
                    metricDelta(b == null ? null : b.errorCount(),      c == null ? null : c.errorCount()),
                    metricDelta(b == null ? null : b.totalDurationMs(), c == null ? null : c.totalDurationMs()),
                    metricDelta(b == null ? null : b.avgDurationMs(),   c == null ? null : c.avgDurationMs()),
                    metricDelta(b == null ? null : b.maxDurationMs(),   c == null ? null : c.maxDurationMs()),
                    metricDelta(b == null ? null : b.timeoutCount(),    c == null ? null : c.timeoutCount())
            ));
        }
        return result;
    }

    private MetricDelta metricDelta(Long base, Long compare) {
        Long deltaValue = (base != null && compare != null) ? compare - base : null;
        Double pct = null;
        if (base != null && base != 0 && compare != null) {
            double raw = (compare - base) * 100.0 / base;
            pct = Math.abs(raw) >= 1.0 ? raw : null;
        }
        return new MetricDelta(base, compare, deltaValue, pct);
    }

    private Map<DimKey, MergedStatsTableRow> index(List<MergedStatsTableRow> rows, Set<RuleNodeDimension> dims) {
        Map<DimKey, MergedStatsTableRow> map = new LinkedHashMap<>();
        for (MergedStatsTableRow r : rows) {
            map.put(dimKey(r.queueId(), r.ruleChainId(), r.ruleNodeId(), r.serviceId(), dims), r);
        }
        return map;
    }

    private List<MergedStatsTableRow> fetchAndJoin(
            UUID tenantId, long startTs, long endTs,
            Set<RuleNodeDimension> dims, Set<RuleNodeDimension> queueDims,
            List<UUID> queueIds, List<UUID> ruleChainIds, List<UUID> ruleNodeIds, List<String> serviceIds) {
        List<RuleNodeTableRow> nodeRows = dao.findRuleNodeTable(
                new RuleNodeTableQuery(tenantId, startTs, endTs, dims, queueIds, ruleChainIds, ruleNodeIds, serviceIds));
        List<QueueTableEntry> queueRows = dao.findQueueTable(
                new QueueTableQuery(tenantId, startTs, endTs, queueDims, queueIds, ruleChainIds, ruleNodeIds, serviceIds));

        return joinStats(nodeRows, queueRows, dims);
    }

    private List<MergedStatsTableRow> joinStats(
            List<RuleNodeTableRow> nodeRows,
            List<QueueTableEntry> queueRows,
            Set<RuleNodeDimension> dims) {
        Map<DimKey, MergedStatsTableRow> merged = new LinkedHashMap<>();

        for (RuleNodeTableRow n : nodeRows) {
            DimKey k = dimKey(n.queueId(), n.ruleChainId(), n.ruleNodeId(), n.serviceId(), dims);
            merged.put(k, new MergedStatsTableRow(
                    k.queueId(), k.ruleChainId(), k.ruleNodeId(), k.serviceId(),
                    n.execCount(), n.errorCount(), n.totalDurationMs(), n.avgDurationMs(), n.maxDurationMs(),
                    null
            ));
        }

        for (QueueTableEntry q : queueRows) {
            DimKey k = dimKey(q.queueId(), q.ruleChainId(), q.ruleNodeId(), q.serviceId(), dims);
            MergedStatsTableRow existing = merged.get(k);
            if (existing != null) {
                merged.put(k, new MergedStatsTableRow(
                        existing.queueId(), existing.ruleChainId(), existing.ruleNodeId(), existing.serviceId(),
                        existing.execCount(), existing.errorCount(), existing.totalDurationMs(),
                        existing.avgDurationMs(), existing.maxDurationMs(),
                        q.timeoutCount()
                ));
            } else {
                merged.put(k, new MergedStatsTableRow(
                        k.queueId(), k.ruleChainId(), k.ruleNodeId(), k.serviceId(),
                        null, null, null, null, null,
                        q.timeoutCount()
                ));
            }
        }

        return new ArrayList<>(merged.values());
    }

    private DimKey dimKey(UUID queueId, UUID ruleChainId, UUID ruleNodeId, String serviceId, Set<RuleNodeDimension> dims) {
        return new DimKey(
                dims.contains(RuleNodeDimension.QUEUE_ID)      ? queueId     : null,
                dims.contains(RuleNodeDimension.RULE_CHAIN_ID) ? ruleChainId : null,
                dims.contains(RuleNodeDimension.RULE_NODE_ID)  ? ruleNodeId  : null,
                dims.contains(RuleNodeDimension.SERVICE_ID)    ? serviceId   : null
        );
    }

    private record DimKey(UUID queueId, UUID ruleChainId, UUID ruleNodeId, String serviceId) {}
}
