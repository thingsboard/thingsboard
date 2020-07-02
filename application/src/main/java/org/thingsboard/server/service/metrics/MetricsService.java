package org.thingsboard.server.service.metrics;

import io.micrometer.core.instrument.DistributionSummary;
import org.thingsboard.server.service.stats.StatsType;

public interface MetricsService {
    DistributionSummary getSummary(StatsType statsType, String key);
}
