// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.alarm.rule.condition.schedule;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

@Schema(
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "ANY_TIME", schema = AnyTimeSchedule.class),
                @DiscriminatorMapping(value = "SPECIFIC_TIME", schema = SpecificTimeSchedule.class),
                @DiscriminatorMapping(value = "CUSTOM", schema = CustomTimeSchedule.class)
        }
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @Type(value = AnyTimeSchedule.class, name = "ANY_TIME"),
        @Type(value = SpecificTimeSchedule.class, name = "SPECIFIC_TIME"),
        @Type(value = CustomTimeSchedule.class, name = "CUSTOM")
})
public interface AlarmSchedule extends Serializable {

    @JsonIgnore
    AlarmScheduleType getType();

}
