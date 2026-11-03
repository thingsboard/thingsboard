// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.housekeeper.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.housekeeper.AlarmsDeletionHousekeeperTask;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.alarm.AlarmService;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlarmsDeletionTaskProcessor extends HousekeeperTaskProcessor<AlarmsDeletionHousekeeperTask> {

    private final AlarmService alarmService;

    @Override
    public void process(AlarmsDeletionHousekeeperTask task) throws Exception {
        EntityId entityId = task.getEntityId();
        EntityType entityType = entityId.getEntityType();
        TenantId tenantId = task.getTenantId();

        if (task.getAlarms() == null) {
            AlarmId lastId = null;
            long lastCreatedTime = 0;
            while (true) {
                List<TbPair<UUID, Long>> alarms = alarmService.findAlarmIdsByOriginatorId(tenantId, entityId, lastCreatedTime, lastId, 128);
                if (alarms.isEmpty()) {
                    break;
                }

                housekeeperClient.submitTask(new AlarmsDeletionHousekeeperTask(tenantId, entityId, alarms.stream().map(TbPair::getFirst).toList()));

                TbPair<UUID, Long> last = alarms.get(alarms.size() - 1);
                lastId = new AlarmId(last.getFirst());
                lastCreatedTime = last.getSecond();
                log.debug("[{}][{}][{}] Submitted task for deleting {} alarms", tenantId, entityType, entityId, alarms.size());
            }
            int count = alarmService.deleteEntityAlarmRecords(tenantId, entityId);
            log.debug("[{}][{}][{}] Deleted {} entity alarms", tenantId, entityType, entityId, count);
        } else {
            for (UUID alarmId : task.getAlarms()) {
                alarmService.delAlarm(tenantId, new AlarmId(alarmId));
            }
            log.debug("[{}][{}][{}] Deleted {} alarms", tenantId, entityType, entityId, task.getAlarms().size());
        }
    }

    @Override
    public HousekeeperTaskType getTaskType() {
        return HousekeeperTaskType.DELETE_ALARMS;
    }

}
