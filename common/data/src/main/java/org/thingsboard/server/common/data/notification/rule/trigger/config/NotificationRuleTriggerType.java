// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.rule.trigger.config;

import lombok.Getter;

@Getter
public enum NotificationRuleTriggerType {

    ENTITY_ACTION,
    ALARM,
    ALARM_COMMENT,
    ALARM_ASSIGNMENT,
    DEVICE_ACTIVITY,
    RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT,
    EDGE_CONNECTION,
    EDGE_COMMUNICATION_FAILURE,
    NEW_PLATFORM_VERSION(false),
    ENTITIES_LIMIT(false),
    API_USAGE_LIMIT(false),
    RATE_LIMITS(false),
    TASK_PROCESSING_FAILURE(false),
    RESOURCES_SHORTAGE(false);

    private final boolean tenantLevel;

    NotificationRuleTriggerType() {
        this(true);
    }

    NotificationRuleTriggerType(boolean tenantLevel) {
        this.tenantLevel = tenantLevel;
    }

}
