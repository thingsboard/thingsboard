// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.stats;

import io.micrometer.core.instrument.Counter;

import java.util.concurrent.atomic.AtomicInteger;

public class StatsCounter extends DefaultCounter {
    private final String name;

    public StatsCounter(AtomicInteger aiCounter, Counter micrometerCounter, String name) {
        super(aiCounter, micrometerCounter);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
