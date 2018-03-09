/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.common.transport.quota.inmemory;


import org.thingsboard.server.common.transport.quota.Clock;

import java.util.concurrent.atomic.LongAdder;

/**
 * @author Vitaliy Paromskiy
 * @version 1.0
 */
public class IntervalCount {

    private final LongAdder adder = new LongAdder();
    private final long intervalDurationMs;
    private volatile long startTime;
    private volatile long lastTickTime;

    public IntervalCount(long intervalDurationMs) {
        this.intervalDurationMs = intervalDurationMs;
        startTime = Clock.millis();
    }

    public long resetIfExpiredAndTick() {
        if (isExpired()) {
            reset();
        }
        tick();
        return adder.sum();
    }

    public long silenceDuration() {
        return Clock.millis() - lastTickTime;
    }

    public long getCount() {
        return adder.sum();
    }

    private void tick() {
        adder.add(1);
        lastTickTime = Clock.millis();
    }

    private void reset() {
        adder.reset();
        startTime = Clock.millis();
    }

    private boolean isExpired() {
        return (Clock.millis() - startTime) > intervalDurationMs;
    }
}
