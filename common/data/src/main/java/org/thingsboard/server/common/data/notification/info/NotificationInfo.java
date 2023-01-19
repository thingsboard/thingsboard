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
package org.thingsboard.server.common.data.notification.info;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.UserId;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "originatorType")
@JsonSubTypes({
        @Type(name = "USER", value = UserOriginatedNotificationInfo.class),
        @Type(name = "ALARM", value = AlarmNotificationInfo.class),
        @Type(name = "RULE_CHAIN", value = RuleEngineOriginatedNotificationInfo.class),
        @Type(name = "DEVICE", value = DeviceInactivityNotificationInfo.class),
        @Type(name = "TENANT", value = EntityActionNotificationInfo.class)
})
public interface NotificationInfo {

    @JsonIgnore
    EntityType getOriginatorType(); // FIXME: originatorType is bad identifier, might have 2 types of info related to device

    @JsonIgnore
    Map<String, String> getTemplateData();

}
