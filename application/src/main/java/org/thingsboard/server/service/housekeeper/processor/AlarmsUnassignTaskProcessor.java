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
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.dao.housekeeper.data.HousekeeperTaskType;
import org.thingsboard.server.dao.housekeeper.data.AlarmsUnassignHousekeeperTask;
import org.thingsboard.server.service.entitiy.alarm.TbAlarmService;

@Component
@RequiredArgsConstructor
public class AlarmsUnassignTaskProcessor implements HousekeeperTaskProcessor<AlarmsUnassignHousekeeperTask> {

    private final TbAlarmService alarmService;

    @Override
    public void process(AlarmsUnassignHousekeeperTask task) throws Exception {
        alarmService.unassignDeletedUserAlarms(task.getTenantId(), (UserId) task.getEntityId(), task.getUserTitle(), task.getTs());
    }

    @Override
    public HousekeeperTaskType getTaskType() {
        return HousekeeperTaskType.UNASSIGN_ALARMS;
    }

}
