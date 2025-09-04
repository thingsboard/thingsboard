/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.common.data.cf.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public interface ScheduledUpdateSupportedCalculatedFieldConfiguration extends CalculatedFieldConfiguration {

    Set<TimeUnit> SUPPORTED_TIME_UNITS =
            EnumSet.of(TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS);


    @JsonIgnore
    boolean isScheduledUpdateEnabled();

    int getScheduledUpdateInterval();

    void setScheduledUpdateInterval(int interval);

    TimeUnit getTimeUnit();

    void setTimeUnit(TimeUnit timeUnit);

    default void validate(long minAllowedScheduledUpdateInterval) {
        var timeUnit = getTimeUnit();
        if (timeUnit == null) {
            throw new IllegalArgumentException("Scheduled update time unit should be specified!");
        }
        if (!SUPPORTED_TIME_UNITS.contains(timeUnit)) {
            throw new IllegalArgumentException("Unsupported scheduled update time unit: " + timeUnit +
                                               ". Allowed: " + SUPPORTED_TIME_UNITS);
        }
        if (timeUnit.toSeconds(getScheduledUpdateInterval()) < minAllowedScheduledUpdateInterval) {
            throw new IllegalArgumentException("Scheduled update interval is less than configured " +
                                               "minimum allowed interval in tenant profile: " + minAllowedScheduledUpdateInterval);
        }
    }
}
