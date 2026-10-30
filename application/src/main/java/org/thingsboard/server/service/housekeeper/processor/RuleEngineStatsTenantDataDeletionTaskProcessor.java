// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.housekeeper.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTask;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.RuleEngineMonitoringDao;

/**
 * Deletes a deleted tenant's rule engine monitoring stats, including those of its own (isolated) queues.
 */
@Component
@RequiredArgsConstructor
public class RuleEngineStatsTenantDataDeletionTaskProcessor extends HousekeeperTaskProcessor<HousekeeperTask> {

    private final RuleEngineMonitoringDao ruleEngineMonitoringDao;

    @Override
    public void process(HousekeeperTask task) throws Exception {
        ruleEngineMonitoringDao.deleteByTenantId(task.getTenantId().getId());
    }

    @Override
    public HousekeeperTaskType getTaskType() {
        return HousekeeperTaskType.DELETE_RULE_ENGINE_STATS_TENANT_DATA;
    }

}
