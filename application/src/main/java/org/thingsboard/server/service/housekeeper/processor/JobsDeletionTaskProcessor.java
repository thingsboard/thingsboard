// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.housekeeper.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTask;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;
import org.thingsboard.server.dao.job.JobService;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobsDeletionTaskProcessor extends HousekeeperTaskProcessor<HousekeeperTask> {

    private final JobService jobService;

    @Override
    public void process(HousekeeperTask task) throws Exception {
        int deletedCount = jobService.deleteJobsByEntityId(task.getTenantId(), task.getEntityId());
        log.debug("[{}][{}][{}] Deleted {} jobs", task.getTenantId(), task.getEntityId().getEntityType(), task.getEntityId(), deletedCount);
    }

    @Override
    public HousekeeperTaskType getTaskType() {
        return HousekeeperTaskType.DELETE_JOBS;
    }

}
