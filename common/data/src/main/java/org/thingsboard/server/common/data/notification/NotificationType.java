// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public enum NotificationType {

    GENERAL,
    ALARM,
    DEVICE_ACTIVITY,
    ENTITY_ACTION,
    ALARM_COMMENT,
    RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT,
    ALARM_ASSIGNMENT,
    NEW_PLATFORM_VERSION,
    ENTITIES_LIMIT,
    API_USAGE_LIMIT,
    RULE_NODE,
    RATE_LIMITS,
    EDGE_CONNECTION,
    EDGE_COMMUNICATION_FAILURE,
    TASK_PROCESSING_FAILURE,
    RESOURCES_SHORTAGE;

    @Getter
    private boolean system; // for future use and compatibility with PE

}
