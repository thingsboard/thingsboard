/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.common.stats;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.StringUtils;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToDoubleFunction;

@Service
public class DefaultStatsFactory implements StatsFactory {
    private static final String TOTAL_MSGS = "totalMsgs";
    private static final String SUCCESSFUL_MSGS = "successfulMsgs";
    private static final String FAILED_MSGS = "failedMsgs";

    private static final String STATS_NAME_TAG = "statsName";

    private static final Counter STUB_COUNTER = new StubCounter();

    @Autowired
    private MeterRegistry meterRegistry;

    @Value("${metrics.enabled:false}")
    private Boolean metricsEnabled;

    @Value("${metrics.timer.percentiles:0.5}")
    private String timerPercentilesStr;

    private double[] timerPercentiles;

    @PostConstruct
    public void init() {
        if (!StringUtils.isEmpty(timerPercentilesStr)) {
            String[] split = timerPercentilesStr.split(",");
            timerPercentiles = new double[split.length];
            for (int i = 0; i < split.length; i++) {
                timerPercentiles[i] = Double.parseDouble(split[i]);
            }
        }
    }


    @Override
    public StatsCounter createStatsCounter(String key, String statsName, String... otherTags) {
        String[] tags = getTags(statsName, otherTags);
        return new StatsCounter(
                new AtomicInteger(0),
                metricsEnabled ? meterRegistry.counter(key, tags) : STUB_COUNTER,
                statsName
        );
    }

    @Override
    public DefaultCounter createDefaultCounter(String key, String... tags) {
        return new DefaultCounter(
                new AtomicInteger(0),
                metricsEnabled ?
                        meterRegistry.counter(key, tags)
                        : STUB_COUNTER
        );
    }

    @Override
    public <T extends Number> T createGauge(String key, T number, String... tags) {
        return meterRegistry.gauge(key, Tags.of(tags), number);
    }

    @Override
    public <T extends Number> T createGauge(String type, String name, T number, String... tags) {
        return createGauge(type, number, getTags(name, tags));
    }

    @Override
    public <S> void createGauge(String type, String name, S stateObject, ToDoubleFunction<S> numberProvider, String... tags) {
        meterRegistry.gauge(type, Tags.of(getTags(name, tags)), stateObject, numberProvider);
    }

    @Override
    public MessagesStats createMessagesStats(String key) {
        StatsCounter totalCounter = createStatsCounter(key, TOTAL_MSGS);
        StatsCounter successfulCounter = createStatsCounter(key, SUCCESSFUL_MSGS);
        StatsCounter failedCounter = createStatsCounter(key, FAILED_MSGS);
        return new DefaultMessagesStats(totalCounter, successfulCounter, failedCounter);
    }

    @Override
    public Timer createTimer(String key, String... tags) {
        Timer.Builder timerBuilder = Timer.builder(key)
                .tags(tags)
                .publishPercentiles();
        if (timerPercentiles != null && timerPercentiles.length > 0) {
            timerBuilder.publishPercentiles(timerPercentiles);
        }
        return timerBuilder.register(meterRegistry);
    }

    @Override
    public StatsTimer createStatsTimer(String type, String name, String... tags) {
        return new StatsTimer(name, Timer.builder(type)
                .tags(getTags(name, tags))
                .register(meterRegistry));
    }

    private static String[] getTags(String statsName, String[] otherTags) {
        String[] tags = new String[]{STATS_NAME_TAG, statsName};
        if (otherTags.length > 0) {
            if (otherTags.length % 2 != 0) {
                throw new IllegalArgumentException("Invalid tags array size");
            }
            tags = ArrayUtils.addAll(tags, otherTags);
        }
        return tags;
    }

    private static class StubCounter implements Counter {
        @Override
        public void increment(double amount) {
        }

        @Override
        public double count() {
            return 0;
        }

        @Override
        public Id getId() {
            return null;
        }
    }
}
