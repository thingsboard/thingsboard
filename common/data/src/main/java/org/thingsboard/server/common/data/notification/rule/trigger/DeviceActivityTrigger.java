// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.rule.trigger;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;

@Data
@Builder
public class DeviceActivityTrigger implements NotificationRuleTrigger {

    private final TenantId tenantId;
    private final CustomerId customerId;
    private final DeviceId deviceId;
    private final boolean active;

    private final String deviceName;
    private final String deviceType;
    private final String deviceLabel;

    @Override
    public EntityId getOriginatorEntityId() {
        return deviceId;
    }

    @Override
    public NotificationRuleTriggerType getType() {
        return NotificationRuleTriggerType.DEVICE_ACTIVITY;
    }

}
