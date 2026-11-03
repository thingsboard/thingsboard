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
public class EdgeCommunicationFailureNotificationRuleTriggerConfig implements NotificationRuleTriggerConfig {

    private Set<UUID> edges; // if empty - all edges

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.EDGE_COMMUNICATION_FAILURE;
    }

}
