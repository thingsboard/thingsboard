// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.housekeeper.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.housekeeper.AlarmCommentsDeletionHousekeeperTask;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;
import org.thingsboard.server.dao.alarm.AlarmCommentDao;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlarmCommentsDeletionTaskProcessor extends HousekeeperTaskProcessor<AlarmCommentsDeletionHousekeeperTask> {

    private final AlarmCommentDao alarmCommentDao;

    @Override
    public void process(AlarmCommentsDeletionHousekeeperTask task) throws Exception {
        // A task without a batch is the single-alarm cleanup that CleanUpService still produces today: entityId is the alarm.
        List<UUID> alarmIds = task.getAlarms() != null ? task.getAlarms() : List.of(task.getEntityId().getId());
        int deleted = alarmCommentDao.deleteByAlarmIds(alarmIds);
        log.debug("[{}][{}] Deleted {} alarm comments for {} alarms", task.getTenantId(), task.getEntityId(), deleted, alarmIds.size());
    }

    @Override
    public HousekeeperTaskType getTaskType() {
        return HousekeeperTaskType.DELETE_ALARM_COMMENTS;
    }

}
