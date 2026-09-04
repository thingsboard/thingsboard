// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.rule.trigger.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EdgeConnectionNotificationRuleTriggerConfig implements NotificationRuleTriggerConfig {

    private Set<UUID> edges; // if empty - all edges
    private Set<EdgeConnectivityEvent> notifyOn;

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.EDGE_CONNECTION;
    }

    public enum EdgeConnectivityEvent {
        CONNECTED, DISCONNECTED
    }

}
