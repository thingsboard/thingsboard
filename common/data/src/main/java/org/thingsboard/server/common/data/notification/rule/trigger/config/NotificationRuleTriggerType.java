/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
