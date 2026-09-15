// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

@Schema(
        description = "Specification for alarm conditions",
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "SIMPLE", schema = SimpleAlarmConditionSpec.class),
                @DiscriminatorMapping(value = "DURATION", schema = DurationAlarmConditionSpec.class),
                @DiscriminatorMapping(value = "REPEATING", schema = RepeatingAlarmConditionSpec.class)
        }
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SimpleAlarmConditionSpec.class, name = "SIMPLE"),
        @JsonSubTypes.Type(value = DurationAlarmConditionSpec.class, name = "DURATION"),
        @JsonSubTypes.Type(value = RepeatingAlarmConditionSpec.class, name = "REPEATING")})
@Deprecated
public interface AlarmConditionSpec extends Serializable {

    @JsonIgnore
    AlarmConditionSpecType getType();

}
