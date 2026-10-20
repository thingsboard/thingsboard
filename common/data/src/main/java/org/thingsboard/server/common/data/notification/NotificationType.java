// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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
    ENTITIES_LIMIT_INCREASE_REQUEST(true),
    ADDON_ACCESS_REQUEST(true),
    ADDON_ACCESS_ERROR(true),
    PLAN_UPGRADE_REQUEST(true),
    API_USAGE_LIMIT,
    RULE_NODE,
    INTEGRATION_LIFECYCLE_EVENT,
    RATE_LIMITS,
    EDGE_CONNECTION,
    EDGE_COMMUNICATION_FAILURE,
    TASK_PROCESSING_FAILURE,
    RESOURCES_SHORTAGE,
    USER_ACTIVATED(true),
    USER_REGISTERED(true),
    REPORT_GENERATED;

    @Getter
    private boolean system;

}
