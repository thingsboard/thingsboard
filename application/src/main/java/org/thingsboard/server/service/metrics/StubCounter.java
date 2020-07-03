package org.thingsboard.server.service.metrics;

import io.micrometer.core.instrument.Counter;

public class StubCounter implements Counter {
    @Override
    public void increment(double amount) {}

    @Override
    public double count() {
        return 0;
    }

    @Override
    public Id getId() {
        return null;
    }
}
