package org.thingsboard.server.service.stats;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.service.metrics.StubCounter;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class StatsCounterFactory {
    private static final String STATS_NAME_TAG = "statsName";

    // TODO not sure if that's a good idea
    private static final Counter STUB_COUNTER = new StubCounter();

    @Autowired
    private MeterRegistry meterRegistry;

    @Value("${metrics.enabled}")
    private Boolean metricsEnabled;

    public StatsCounter createStatsCounter(String key, String statsName) {
        return new StatsCounter(
                new AtomicInteger(0),
                metricsEnabled ?
                        meterRegistry.counter(key, STATS_NAME_TAG, statsName)
                        : STUB_COUNTER,
                statsName
        );
    }
}
