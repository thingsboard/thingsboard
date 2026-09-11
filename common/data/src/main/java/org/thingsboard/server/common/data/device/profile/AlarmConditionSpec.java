// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SimpleAlarmConditionSpec.class, name = "SIMPLE"),
        @JsonSubTypes.Type(value = DurationAlarmConditionSpec.class, name = "DURATION"),
        @JsonSubTypes.Type(value = RepeatingAlarmConditionSpec.class, name = "REPEATING")})
public interface AlarmConditionSpec extends Serializable {

    @JsonIgnore
    AlarmConditionSpecType getType();

}
