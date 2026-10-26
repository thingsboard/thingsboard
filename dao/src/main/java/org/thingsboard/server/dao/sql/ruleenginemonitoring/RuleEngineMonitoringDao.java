// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.ruleenginemonitoring;

import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.QueueLagTableEntry;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.QueueLagTimeseriesEntry;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.QueueTableEntry;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.QueueTimeseriesEntry;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.RuleNodeTableRow;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.RuleNodeTsKvEntry;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface RuleEngineMonitoringDao {
    void saveRuleNodeStats(List<RuleNodeExecStatsRow> rows);

    List<RuleNodeTsKvEntry> findRuleNodeTimeseries(RuleNodeTimeseriesQuery query);

    List<RuleNodeTableRow> findRuleNodeTable(RuleNodeTableQuery query);

    void saveQueueStats(List<QueueStatsRow> rows);

    List<QueueTimeseriesEntry> findQueueTimeseries(QueueTimeseriesQuery query);

    List<QueueTableEntry> findQueueTable(QueueTableQuery query);

    void saveQueueLagStats(List<QueueLagStatsRow> rows);

    boolean tryAcquireQueueLagLeader(String serviceId, long nowMs, long leaseMs);

    void deleteByTenantId(UUID tenantId);

    /**
     * Deletes, in batches, all rule engine stats bucketed before {@code expTime}.
     */
    void cleanUp(long expTime, int batchSize);

    List<QueueLagTimeseriesEntry> findQueueLagTimeseries(QueueLagTimeseriesQuery query);

    List<QueueLagTableEntry> findQueueLagTable(QueueLagTableQuery query);

    /**
     * Sum of each queue's latest lag; a queue with no row in the last {@code staleAfterMs} counts as 0.
     */
    long findLastQueueLag(UUID tenantId, List<UUID> queueIds, long staleAfterMs);

    Map<UUID, UUID> findRuleChainIdsByRuleNodeIds(Collection<UUID> ruleNodeIds);

    List<QueueShortInfo> findAllQueuesShortInfo();

    List<RuleEngineMonitoringQueueOption> findFilterQueues(UUID tenantId);

    List<RuleEngineMonitoringOption> findFilterRuleChains(UUID tenantId);

    List<RuleEngineMonitoringRuleNodeOption> findFilterRuleNodes(UUID tenantId);
}
