// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.rule.trigger.config;

import jakarta.validation.constraints.Max;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResourcesShortageNotificationRuleTriggerConfig implements NotificationRuleTriggerConfig {

    @Serial
    private static final long serialVersionUID = 339395299693241424L;

    @Max(1)
    private float cpuThreshold; // in percents
    @Max(1)
    private float ramThreshold; // in percents
    @Max(1)
    private float storageThreshold; // in percents

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.RESOURCES_SHORTAGE;
    }

}
