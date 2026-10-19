// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.stats;

import io.micrometer.core.instrument.Counter;

import java.util.concurrent.atomic.AtomicInteger;

public class StatsCounter extends DefaultCounter implements org.thingsboard.server.common.stats.Counter {
    private final String name;

    public StatsCounter(AtomicInteger aiCounter, Counter micrometerCounter, String name) {
        super(aiCounter, micrometerCounter);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
