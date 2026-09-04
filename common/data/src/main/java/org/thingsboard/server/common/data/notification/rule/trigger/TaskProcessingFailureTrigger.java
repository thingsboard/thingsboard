// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.rule.trigger;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTask;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;

import java.io.Serial;

@Data
@Builder
public class TaskProcessingFailureTrigger implements NotificationRuleTrigger {

    @Serial
    private static final long serialVersionUID = 5606203770553105345L;

    private final HousekeeperTask task;
    private final int attempt;
    private final Throwable error;

    @Override
    public NotificationRuleTriggerType getType() {
        return NotificationRuleTriggerType.TASK_PROCESSING_FAILURE;
    }

    @Override
    public TenantId getTenantId() {
        return task.getTenantId();
    }

    @Override
    public EntityId getOriginatorEntityId() {
        return task.getEntityId();
    }

}
