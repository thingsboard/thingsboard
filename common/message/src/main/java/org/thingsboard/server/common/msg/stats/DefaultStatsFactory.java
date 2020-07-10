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
package org.thingsboard.server.common.msg.stats;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

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

    @Override
    public StatsCounter createStatsCounter(String key, String statsName) {
        return new StatsCounter(
                new AtomicInteger(0),
                metricsEnabled ?
                        meterRegistry.counter(key, STATS_NAME_TAG, statsName)
                        : STUB_COUNTER,
                statsName
        );
    }

    @Override
    public MessagesStats createMessagesStats(String key) {
        StatsCounter totalCounter = createStatsCounter(key, TOTAL_MSGS);
        StatsCounter successfulCounter = createStatsCounter(key, SUCCESSFUL_MSGS);
        StatsCounter failedCounter = createStatsCounter(key, FAILED_MSGS);
        return new DefaultMessagesStats(totalCounter, successfulCounter, failedCounter);
    }

    private static class StubCounter implements Counter {
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
}
