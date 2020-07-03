package org.thingsboard.server.service.stats;

import io.micrometer.core.instrument.Counter;

import java.util.concurrent.atomic.AtomicInteger;

public class StatsCounter {
    private final AtomicInteger aiCounter;
    private final Counter micrometerCounter;
    private final String name;

    public StatsCounter(AtomicInteger aiCounter, Counter micrometerCounter, String name) {
        this.aiCounter = aiCounter;
        this.micrometerCounter = micrometerCounter;
        this.name = name;
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

    public void add(int delta){
        aiCounter.addAndGet(delta);
        micrometerCounter.increment(delta);
    }

    public String getName() {
        return name;
    }
}
