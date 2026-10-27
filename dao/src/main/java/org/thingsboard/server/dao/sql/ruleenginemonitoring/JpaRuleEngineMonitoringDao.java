// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.ruleenginemonitoring;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.dao.config.DefaultDataSource;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.QueueLagTableEntry;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.QueueLagTimeseriesEntry;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.QueueTableEntry;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.QueueTimeseriesEntry;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.RuleNodeTableRow;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.RuleNodeTsKvEntry;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@DefaultDataSource
@Component
@SqlDao
@RequiredArgsConstructor
public class JpaRuleEngineMonitoringDao implements RuleEngineMonitoringDao {

    private final RuleEngineStatsInsertRepository statsInsertRepository;
    private final RuleEngineStatsQueryRepository statsQueryRepository;
    private final RuleEngineMonitoringLookupRepository lookupRepository;

    @Override
    public void saveRuleNodeStats(List<RuleNodeExecStatsRow> rows) {
        statsInsertRepository.saveRuleNodeStats(rows);
    }

    @Override
    public List<RuleNodeTsKvEntry> findRuleNodeTimeseries(RuleNodeTimeseriesQuery query) {
        return statsQueryRepository.findRuleNodeTimeseries(query);
    }

    @Override
    public List<RuleNodeTableRow> findRuleNodeTable(RuleNodeTableQuery query) {
        return statsQueryRepository.findRuleNodeTable(query);
    }

    @Override
    public void saveQueueStats(List<QueueStatsRow> rows) {
        statsInsertRepository.saveQueueStats(rows);
    }

    @Override
    public List<QueueTimeseriesEntry> findQueueTimeseries(QueueTimeseriesQuery query) {
        return statsQueryRepository.findQueueTimeseries(query);
    }

    @Override
    public List<QueueTableEntry> findQueueTable(QueueTableQuery query) {
        return statsQueryRepository.findQueueTable(query);
    }

    @Override
    public void saveQueueLagStats(List<QueueLagStatsRow> rows) {
        statsInsertRepository.saveQueueLagStats(rows);
    }

    @Override
    public void deleteByTenantId(UUID tenantId) {
        statsInsertRepository.deleteByTenantId(tenantId);
    }

    @Override
    public void cleanUp(long expTime, int batchSize) {
        statsInsertRepository.cleanUp(expTime, batchSize);
    }

    @Override
    public List<QueueLagTimeseriesEntry> findQueueLagTimeseries(QueueLagTimeseriesQuery query) {
        return statsQueryRepository.findQueueLagTimeseries(query);
    }

    @Override
    public List<QueueLagTableEntry> findQueueLagTable(QueueLagTableQuery query) {
        return statsQueryRepository.findQueueLagTable(query);
    }

    @Override
    public long findLastQueueLag(UUID tenantId, List<UUID> queueIds, long staleAfterMs) {
        return statsQueryRepository.findLastQueueLag(tenantId, queueIds, staleAfterMs);
    }

    @Override
    public Map<UUID, UUID> findRuleChainIdsByRuleNodeIds(Collection<UUID> ruleNodeIds) {
        return lookupRepository.findRuleChainIdsByRuleNodeIds(ruleNodeIds);
    }

    @Override
    public List<QueueShortInfo> findAllQueuesShortInfo() {
        return lookupRepository.findAllQueuesShortInfo();
    }

    @Override
    public List<RuleEngineMonitoringQueueOption> findFilterQueues(UUID tenantId) {
        return lookupRepository.findFilterQueues(tenantId);
    }

    @Override
    public List<RuleEngineMonitoringOption> findFilterRuleChains(UUID tenantId) {
        return lookupRepository.findFilterRuleChains(tenantId);
    }

    @Override
    public List<RuleEngineMonitoringRuleNodeOption> findFilterRuleNodes(UUID tenantId) {
        return lookupRepository.findFilterRuleNodes(tenantId);
    }

}
