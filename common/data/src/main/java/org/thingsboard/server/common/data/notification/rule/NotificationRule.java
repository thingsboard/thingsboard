/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.common.data.notification.rule;

import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class NotificationRule {

    // we may choose it in the alarm rule config, or maybe it's better to configure evrth in the triggers?

    private UUID id; // NotificationRuleId id;
    private String name;
    private Map<String, Object> triggers; // or maybe bad idea
    // Map<NotificationTriggerType, NotificationTriggerConfig> - concrete alarmRule or alarm rule search (e.g. alarm rule of device profiles of particular transport type with certain severity)
    // triggerConfiguration (??) - alarm filter: severity, specific device profile, alarm rule

    private UUID initialNotificationTargetId;
    private List<NonConfirmedNotificationEscalation> escalations;

}
