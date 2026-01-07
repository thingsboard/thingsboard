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
package org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.ZoneId;
import java.time.ZonedDateTime;

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
