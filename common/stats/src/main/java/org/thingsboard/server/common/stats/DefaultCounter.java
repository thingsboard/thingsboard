// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.stats;

import io.micrometer.core.instrument.Counter;

import java.util.concurrent.atomic.AtomicInteger;

public class DefaultCounter {
    private final AtomicInteger aiCounter;
    private final Counter micrometerCounter;

    public DefaultCounter(AtomicInteger aiCounter, Counter micrometerCounter) {
        this.aiCounter = aiCounter;
        this.micrometerCounter = micrometerCounter;
    }

    public void increment() {
        aiCounter.incrementAndGet();
        micrometerCounter.increment();
    }

    public void clear() {
        aiCounter.set(0);
    }

    public int get() {
        return aiCounter.get();
    }

    public int getAndClear() {
        return aiCounter.getAndSet(0);
    }

    public void add(int delta){
        aiCounter.addAndGet(delta);
        micrometerCounter.increment(delta);
    }
}
