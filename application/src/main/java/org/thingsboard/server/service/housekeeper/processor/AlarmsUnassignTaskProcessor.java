// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
