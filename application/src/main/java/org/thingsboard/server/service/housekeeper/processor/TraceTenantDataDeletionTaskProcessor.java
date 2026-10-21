// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.housekeeper.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTask;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.RuleEngineMonitoringDao;
import org.thingsboard.server.dao.sql.trace.TraceDao;

/**
 * Deletes a deleted tenant's traces and rule engine monitoring stats. The tenant's coverage setting lives in the main
 * database and is removed with the tenant itself.
 */
@Component
@RequiredArgsConstructor
public class TraceTenantDataDeletionTaskProcessor extends HousekeeperTaskProcessor<HousekeeperTask> {

    private final TraceDao traceDao;
    private final RuleEngineMonitoringDao ruleEngineMonitoringDao;

    @Override
    public void process(HousekeeperTask task) throws Exception {
        traceDao.deleteByTenantId(task.getTenantId().getId());
        ruleEngineMonitoringDao.deleteByTenantId(task.getTenantId().getId());
    }

    @Override
    public HousekeeperTaskType getTaskType() {
        return HousekeeperTaskType.DELETE_TRACE_TENANT_DATA;
    }

}
