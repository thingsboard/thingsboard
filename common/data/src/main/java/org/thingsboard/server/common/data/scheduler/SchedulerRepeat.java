// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.scheduler;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Created by ashvayka on 28.11.17.
 */
@Schema(
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "DAILY", schema = DailyRepeat.class),
                @DiscriminatorMapping(value = "EVERY_N_DAYS", schema = EveryNDaysRepeat.class),
                @DiscriminatorMapping(value = "WEEKLY", schema = WeeklyRepeat.class),
                @DiscriminatorMapping(value = "EVERY_N_WEEKS", schema = EveryNWeeksRepeat.class),
                @DiscriminatorMapping(value = "MONTHLY", schema = MonthlyRepeat.class),
                @DiscriminatorMapping(value = "YEARLY", schema = YearlyRepeat.class),
                @DiscriminatorMapping(value = "TIMER", schema = TimerRepeat.class)
        }
)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DailyRepeat.class, name = "DAILY"),
        @JsonSubTypes.Type(value = EveryNDaysRepeat.class, name = "EVERY_N_DAYS"),
        @JsonSubTypes.Type(value = WeeklyRepeat.class, name = "WEEKLY"),
        @JsonSubTypes.Type(value = EveryNWeeksRepeat.class, name = "EVERY_N_WEEKS"),
        @JsonSubTypes.Type(value = MonthlyRepeat.class, name = "MONTHLY"),
        @JsonSubTypes.Type(value = YearlyRepeat.class, name = "YEARLY"),
        @JsonSubTypes.Type(value = TimerRepeat.class, name = "TIMER")
})
public interface SchedulerRepeat {

    long getEndsOn();

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    SchedulerRepeatType getType();

    long getNext(long startTime, long ts, String timezone);

}
