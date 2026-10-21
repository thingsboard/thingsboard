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
    public boolean tryAcquireQueueLagLeader(String serviceId, long nowMs, long leaseMs) {
        return statsInsertRepository.tryAcquireQueueLagLeader(serviceId, nowMs, leaseMs);
    }

    @Override
    public void deleteByTenantId(UUID tenantId) {
        statsInsertRepository.deleteByTenantId(tenantId);
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
