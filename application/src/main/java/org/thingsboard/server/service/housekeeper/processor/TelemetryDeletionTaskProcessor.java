/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import org.thingsboard.server.common.data.kv.BaseDeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.dao.housekeeper.data.HousekeeperTask;
import org.thingsboard.server.dao.housekeeper.data.HousekeeperTaskType;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelemetryDeletionTaskProcessor implements HousekeeperTaskProcessor<HousekeeperTask> {

    private final TimeseriesService timeseriesService;

    @Override
    public void process(HousekeeperTask task) throws Exception {
        List<String> keys = timeseriesService.findAllKeysByEntityIds(task.getTenantId(), List.of(task.getEntityId()));
        for (String key : keys) {
            DeleteTsKvQuery deleteQuery = new BaseDeleteTsKvQuery(key, 0, System.currentTimeMillis(), false, true);
            timeseriesService.remove(task.getTenantId(), task.getEntityId(), List.of(deleteQuery)).get();
        }
        log.debug("[{}][{}][{}] Deleted {} telemetry keys", task.getTenantId(), task.getEntityId().getEntityType(), task.getEntityId(), keys.size());
    }

    @Override
    public HousekeeperTaskType getTaskType() {
        return HousekeeperTaskType.DELETE_TELEMETRY;
    }

}
