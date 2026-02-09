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
package org.thingsboard.server.common.stats;

import io.micrometer.core.instrument.Timer;
import lombok.Getter;

import java.util.concurrent.TimeUnit;

public class StatsTimer {

    @Getter
    private final String name;
    private final Timer timer;

    private int count;
    private long totalTime;

    public StatsTimer(String name, Timer micrometerTimer) {
        this.name = name;
        this.timer = micrometerTimer;
    }

    public void record(long timeMs) {
        record(timeMs, TimeUnit.MILLISECONDS);
    }

    public void record(long timing, TimeUnit timeUnit) {
        count++;
        totalTime += timeUnit.toMillis(timing);
        timer.record(timing, timeUnit);
    }

    public double getAvg() {
        if (count == 0) {
            return 0.0;
        }
        return (double) totalTime / count;
    }

    public void reset() {
        count = 0;
        totalTime = 0;
    }

}
