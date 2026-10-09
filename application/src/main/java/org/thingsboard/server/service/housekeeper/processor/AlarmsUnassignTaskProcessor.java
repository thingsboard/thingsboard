// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.housekeeper.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.alarm.AlarmRef;
import org.thingsboard.server.common.data.housekeeper.AlarmsUnassignHousekeeperTask;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.sql.citus.CitusSettings;
import org.thingsboard.server.service.entitiy.alarm.TbAlarmService;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlarmsUnassignTaskProcessor extends HousekeeperTaskProcessor<AlarmsUnassignHousekeeperTask> {

    private final TbAlarmService tbAlarmService;
    private final AlarmService alarmService;
    private final CitusSettings citusSettings;

    @Override
    public void process(AlarmsUnassignHousekeeperTask task) throws Exception {
        TenantId tenantId = task.getTenantId();
        UserId userId = (UserId) task.getEntityId();
        if (citusSettings.isEnabled()) {
            // Clear every assignment for the deleted user in a single multi-shard statement; see
            // BaseAlarmService.unassignAlarmsByAssignee for why the per-alarm side effects are intentionally skipped here.
            int count = alarmService.unassignAlarmsByAssignee(tenantId, userId, task.getTs());
            log.debug("[{}][{}] Unassigned {} alarms from deleted user (Citus bulk)", tenantId, userId, count);
            return;
        }
        // Otherwise preserve the original per-alarm behavior (system comment + audit + notification). The root task
        // (no batch) paginates the assignee index into per-batch tasks; each batch task then unassigns its alarms.
        if (task.getAlarmRefs() == null && task.getAlarms() == null) {
            long lastCreatedTime = 0L;
            AlarmId lastId = null;
            while (true) {
                List<AlarmRef> refs = alarmService.findAlarmRefsByAssigneeId(tenantId, userId, lastCreatedTime, lastId, 64);
                if (refs.isEmpty()) {
                    break;
                }
                housekeeperClient.submitTask(new AlarmsUnassignHousekeeperTask(tenantId, userId, task.getUserTitle(), refs));
                AlarmRef lastRef = refs.get(refs.size() - 1);
                lastCreatedTime = lastRef.createdTime();
                lastId = lastRef.alarmId();
                log.debug("[{}][{}] Submitted task for unassigning {} alarms", tenantId, userId, refs.size());
            }
        } else if (task.getAlarmRefs() != null) {
            tbAlarmService.unassignDeletedUserAlarms(tenantId, userId, task.getUserTitle(), task.getAlarmRefs(), task.getTs());
            log.debug("[{}][{}] Unassigned {} alarms", tenantId, userId, task.getAlarmRefs().size());
        } else {
            // In-flight task from an older node carries only alarm ids; the originator is resolved per alarm.
            // For backward compatibility with pre-4.3.1.4 tasks only, remove with the deprecated fallback API.
            tbAlarmService.unassignDeletedUserAlarmsByIds(tenantId, userId, task.getUserTitle(), task.getAlarms(), task.getTs());
            log.debug("[{}][{}] Unassigned {} alarms (legacy payload)", tenantId, userId, task.getAlarms().size());
        }
    }

    @Override
    public HousekeeperTaskType getTaskType() {
        return HousekeeperTaskType.UNASSIGN_ALARMS;
    }

}
