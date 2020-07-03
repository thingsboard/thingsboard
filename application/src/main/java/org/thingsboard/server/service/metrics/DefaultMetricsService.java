/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
