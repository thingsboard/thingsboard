// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Schema(
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "HOUR", schema = HourInterval.class),
                @DiscriminatorMapping(value = "DAY", schema = DayInterval.class),
                @DiscriminatorMapping(value = "WEEK", schema = WeekInterval.class),
                @DiscriminatorMapping(value = "WEEK_SUN_SAT", schema = WeekSunSatInterval.class),
                @DiscriminatorMapping(value = "MONTH", schema = MonthInterval.class),
                @DiscriminatorMapping(value = "QUARTER", schema = QuarterInterval.class),
                @DiscriminatorMapping(value = "YEAR", schema = YearInterval.class),
                @DiscriminatorMapping(value = "CUSTOM", schema = CustomInterval.class)
        }
)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = HourInterval.class, name = "HOUR"),
        @JsonSubTypes.Type(value = DayInterval.class, name = "DAY"),
        @JsonSubTypes.Type(value = WeekInterval.class, name = "WEEK"),
        @JsonSubTypes.Type(value = WeekSunSatInterval.class, name = "WEEK_SUN_SAT"),
        @JsonSubTypes.Type(value = MonthInterval.class, name = "MONTH"),
        @JsonSubTypes.Type(value = QuarterInterval.class, name = "QUARTER"),
        @JsonSubTypes.Type(value = YearInterval.class, name = "YEAR"),
        @JsonSubTypes.Type(value = CustomInterval.class, name = "CUSTOM")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AggInterval {

    @JsonIgnore
    AggIntervalType getType();

    @JsonIgnore
    ZoneId getZoneId();

    @JsonIgnore
    long getCurrentIntervalDurationMillis();

    @JsonIgnore
    long getCurrentIntervalStartTs();

    long getDateTimeIntervalStartTs(ZonedDateTime dateTime);

    @JsonIgnore
    long getCurrentIntervalEndTs();

    long getDateTimeIntervalEndTs(ZonedDateTime dateTime);

    ZonedDateTime getNextIntervalStart(ZonedDateTime currentStart);

    void validate();

}
