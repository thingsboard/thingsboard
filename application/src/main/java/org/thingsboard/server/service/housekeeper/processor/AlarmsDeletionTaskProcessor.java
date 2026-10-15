// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.housekeeper.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.housekeeper.AlarmsDeletionHousekeeperTask;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTask;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.alarm.AlarmService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
            Set<String> alarmTypes = new HashSet<>();
            for (UUID alarmId : task.getAlarms()) {
                // entityId is the alarm originator; passing it routes the delete to the single owning shard instead of
                // a cross-shard lookup-by-id. false = this bulk path batches the alarm-type and alarm-comment cleanup
                // (below) once for the whole batch rather than emitting them per alarm.
                AlarmApiCallResult result = alarmService.delAlarm(tenantId, entityId, new AlarmId(alarmId), false);
                if (result.getAlarm() != null) {
                    alarmTypes.add(result.getAlarm().getType());
                }
            }
            alarmService.delAlarmTypes(tenantId, alarmTypes);
            housekeeperClient.submitTask(HousekeeperTask.deleteAlarmComments(tenantId, entityId, task.getAlarms()));
            log.debug("[{}][{}][{}] Deleted {} alarms", tenantId, entityType, entityId, task.getAlarms().size());
        }
    }

    @Override
    public HousekeeperTaskType getTaskType() {
        return HousekeeperTaskType.DELETE_ALARMS;
    }

}
