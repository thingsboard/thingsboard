/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.common.data.device.profile.alarm.rule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmScheduleType;
import org.thingsboard.server.common.data.query.DynamicValue;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = OldAnyTimeSchedule.class, name = "ANY_TIME"),
        @JsonSubTypes.Type(value = OldSpecificTimeSchedule.class, name = "SPECIFIC_TIME"),
        @JsonSubTypes.Type(value = OldCustomTimeSchedule.class, name = "CUSTOM")})
public interface OldAlarmSchedule extends Serializable {

    AlarmScheduleType getType();

    DynamicValue<String> getDynamicValue();

}
