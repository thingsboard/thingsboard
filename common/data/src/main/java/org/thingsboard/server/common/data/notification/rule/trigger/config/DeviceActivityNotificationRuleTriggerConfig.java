// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.rule.trigger.config;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema
public class DeviceActivityNotificationRuleTriggerConfig implements NotificationRuleTriggerConfig {

    @ArraySchema(schema = @Schema(implementation = UUID.class))
    private Set<UUID> devices;
    @ArraySchema(schema = @Schema(implementation = UUID.class))
    private Set<UUID> deviceProfiles; // set either devices or profiles
    @NotEmpty
    @ArraySchema(schema = @Schema(implementation = DeviceEvent.class))
    private Set<DeviceEvent> notifyOn;

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.DEVICE_ACTIVITY;
    }

    @Schema
    public enum DeviceEvent {
        ACTIVE, INACTIVE
    }

}
