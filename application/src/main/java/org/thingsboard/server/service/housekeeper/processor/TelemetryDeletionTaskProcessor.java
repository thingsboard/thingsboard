// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.housekeeper.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTask;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;
import org.thingsboard.server.common.data.housekeeper.LatestTsDeletionHousekeeperTask;
import org.thingsboard.server.common.data.housekeeper.TsHistoryDeletionHousekeeperTask;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelemetryDeletionTaskProcessor extends HousekeeperTaskProcessor<HousekeeperTask> {

    private final TimeseriesService timeseriesService;

    @Override
    public void process(HousekeeperTask task) throws Exception {
        TenantId tenantId = task.getTenantId();
        EntityId entityId = task.getEntityId();
        List<String> keys = timeseriesService.findAllKeysByEntityIds(tenantId, List.of(entityId));

        for (String key : keys) {
            var latestTsDeletionTask = new LatestTsDeletionHousekeeperTask(tenantId, entityId, key);
            housekeeperClient.submitTask(latestTsDeletionTask);

            var tsHistoryDeletionTask = new TsHistoryDeletionHousekeeperTask(tenantId, entityId, key);
            housekeeperClient.submitTask(tsHistoryDeletionTask);
        }

        log.trace("[{}][{}][{}] Submitted latest and ts history deletion tasks for {} keys", tenantId, entityId.getEntityType(), entityId, keys.size());
    }

    @Override
    public HousekeeperTaskType getTaskType() {
        return HousekeeperTaskType.DELETE_TELEMETRY;
    }

}
