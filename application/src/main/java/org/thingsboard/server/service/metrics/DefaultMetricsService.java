package org.thingsboard.server.service.metrics;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.service.stats.StatsType;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class DefaultMetricsService implements MetricsService {

    private final ConcurrentMap<String, DistributionSummary> statsSummaries = new ConcurrentHashMap<>();

    @Autowired
    private MeterRegistry meterRegistry;


    @Override
    public DistributionSummary getSummary(StatsType statsType, String key) {
        String fullKey = statsType.getName() + "." + key;
        return statsSummaries.computeIfAbsent(fullKey, registryKey -> meterRegistry.summary(registryKey));
    }
}
