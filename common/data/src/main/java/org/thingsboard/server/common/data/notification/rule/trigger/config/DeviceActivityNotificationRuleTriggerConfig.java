// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.rule.trigger.config;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeviceActivityNotificationRuleTriggerConfig implements NotificationRuleTriggerConfig {

    private Set<UUID> devices;
    private Set<UUID> deviceProfiles; // set either devices or profiles
    @NotEmpty
    private Set<DeviceEvent> notifyOn;

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.DEVICE_ACTIVITY;
    }

    public enum DeviceEvent {
        ACTIVE, INACTIVE
    }

}
