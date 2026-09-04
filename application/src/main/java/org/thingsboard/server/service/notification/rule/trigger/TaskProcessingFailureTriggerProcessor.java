// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.notification.rule.trigger;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTask;
import org.thingsboard.server.common.data.notification.info.TaskProcessingFailureNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.TaskProcessingFailureTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.notification.rule.trigger.config.TaskProcessingFailureNotificationRuleTriggerConfig;

@Service
public class TaskProcessingFailureTriggerProcessor implements NotificationRuleTriggerProcessor<TaskProcessingFailureTrigger, TaskProcessingFailureNotificationRuleTriggerConfig> {

    @Override
    public boolean matchesFilter(TaskProcessingFailureTrigger trigger, TaskProcessingFailureNotificationRuleTriggerConfig triggerConfig) {
        return true;
    }

    @Override
    public TaskProcessingFailureNotificationInfo constructNotificationInfo(TaskProcessingFailureTrigger trigger) {
        HousekeeperTask task = trigger.getTask();
        return TaskProcessingFailureNotificationInfo.builder()
                .tenantId(task.getTenantId())
                .entityId(task.getEntityId())
                .taskType(task.getTaskType())
                .taskDescription(task.getDescription())
                .error(StringUtils.truncate(ExceptionUtils.getStackTrace(trigger.getError()), 1024))
                .attempt(trigger.getAttempt())
                .build();
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.TASK_PROCESSING_FAILURE;
    }

}
