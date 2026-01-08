/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
