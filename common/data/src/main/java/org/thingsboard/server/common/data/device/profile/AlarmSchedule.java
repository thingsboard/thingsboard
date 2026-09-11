// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.thingsboard.server.common.data.query.DynamicValue;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AnyTimeSchedule.class, name = "ANY_TIME"),
        @JsonSubTypes.Type(value = SpecificTimeSchedule.class, name = "SPECIFIC_TIME"),
        @JsonSubTypes.Type(value = CustomTimeSchedule.class, name = "CUSTOM")})
public interface AlarmSchedule extends Serializable {

    AlarmScheduleType getType();

    DynamicValue<String> getDynamicValue();

}
