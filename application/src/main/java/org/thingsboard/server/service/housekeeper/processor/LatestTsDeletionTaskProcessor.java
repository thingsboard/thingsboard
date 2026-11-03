// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.housekeeper.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;
import org.thingsboard.server.common.data.housekeeper.LatestTsDeletionHousekeeperTask;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LatestTsDeletionTaskProcessor extends HousekeeperTaskProcessor<LatestTsDeletionHousekeeperTask> {

    private final TimeseriesService timeseriesService;

    @Override
    public void process(LatestTsDeletionHousekeeperTask task) throws Exception {
        wait(timeseriesService.removeLatest(task.getTenantId(), task.getEntityId(), List.of(task.getKey())));
        log.debug("[{}][{}][{}] Deleted latest telemetry for key '{}'", task.getTenantId(), task.getEntityId().getEntityType(), task.getEntityId(), task.getKey());
    }

    @Override
    public HousekeeperTaskType getTaskType() {
        return HousekeeperTaskType.DELETE_LATEST_TS;
    }

}
