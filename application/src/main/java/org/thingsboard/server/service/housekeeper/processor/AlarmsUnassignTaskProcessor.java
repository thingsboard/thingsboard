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
import org.thingsboard.server.common.data.housekeeper.AlarmsUnassignHousekeeperTask;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.service.entitiy.alarm.TbAlarmService;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlarmsUnassignTaskProcessor extends HousekeeperTaskProcessor<AlarmsUnassignHousekeeperTask> {

    private final TbAlarmService tbAlarmService;
    private final AlarmService alarmService;

    @Override
    public void process(AlarmsUnassignHousekeeperTask task) throws Exception {
        TenantId tenantId = task.getTenantId();
        UserId userId = (UserId) task.getEntityId();
        if (task.getAlarms() == null) {
            AlarmId lastId = null;
            long lastCreatedTime = 0;
            while (true) {
                List<TbPair<UUID, Long>> alarms = alarmService.findAlarmIdsByAssigneeId(tenantId, userId, lastCreatedTime, lastId, 64);
                if (alarms.isEmpty()) {
                    break;
                }
                housekeeperClient.submitTask(new AlarmsUnassignHousekeeperTask(tenantId, userId, task.getUserTitle(), alarms.stream().map(TbPair::getFirst).toList()));

                TbPair<UUID, Long> last = alarms.get(alarms.size() - 1);
                lastId = new AlarmId(last.getFirst());
                lastCreatedTime = last.getSecond();
                log.debug("[{}][{}] Submitted task for unassigning {} alarms", tenantId, userId, alarms.size());
            }
        } else {
            tbAlarmService.unassignDeletedUserAlarms(tenantId, userId, task.getUserTitle(), task.getAlarms(), task.getTs());
            log.debug("[{}][{}] Unassigned {} alarms", tenantId, userId, task.getAlarms().size());
        }
    }

    @Override
    public HousekeeperTaskType getTaskType() {
        return HousekeeperTaskType.UNASSIGN_ALARMS;
    }

}
