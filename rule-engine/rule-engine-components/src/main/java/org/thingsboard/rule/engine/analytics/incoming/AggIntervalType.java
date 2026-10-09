// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.incoming;

import java.util.concurrent.TimeUnit;

public enum AggIntervalType {

    HOUR(TimeUnit.HOURS),
    DAY(TimeUnit.DAYS),
    WEEK(DAY.getInterval() * 7),
    WEEK_SUN_SAT(WEEK.getInterval()),
    MONTH(DAY.getInterval() * 30),
    YEAR(DAY.getInterval() * 365),
    CUSTOM(0);

    private final long interval;

    AggIntervalType(long interval) {
        this.interval = interval;
    }

    AggIntervalType(TimeUnit timeUnit) {
        this.interval = timeUnit.toMillis(1);
    }

    public long getInterval() {
        return interval;
    }
}
