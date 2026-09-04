// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
