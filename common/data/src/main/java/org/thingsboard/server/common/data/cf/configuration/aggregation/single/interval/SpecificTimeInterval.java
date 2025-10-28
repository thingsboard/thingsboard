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
package org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval;

import java.time.LocalTime;

public class SpecificTimeInterval implements AggInterval {

    public Long startMillis; // start millis since start of day
    public Long endMillis;   // end millis since start of day

    @Override
    public AggIntervalType getType() {
        return AggIntervalType.SPECIFIC_TIME;
    }

    @Override
    public long getDelayUntilIntervalEnd() {
        long nowMillis = LocalTime.now().toNanoOfDay() / 1_000_000L;
        long delayMillis;
        if (nowMillis < endMillis) {
            delayMillis = endMillis - nowMillis; // later today
        } else {
            delayMillis = (24 * 60 * 60 * 1000L - nowMillis) + endMillis; // next day
        }
        return delayMillis;
    }

}
