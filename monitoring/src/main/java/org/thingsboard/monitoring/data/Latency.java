/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.monitoring.data;

import com.google.common.util.concurrent.AtomicDouble;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class Latency {

    private final String key;
    private final AtomicDouble latencySum = new AtomicDouble();
    private final AtomicInteger counter = new AtomicInteger();

    public synchronized void report(double latencyInMs) {
        latencySum.addAndGet(latencyInMs);
        counter.incrementAndGet();
    }

    public synchronized double getAvg() {
        return latencySum.get() / counter.get();
    }

    public synchronized void reset() {
        latencySum.set(0.0);
        counter.set(0);
    }

    public String getKey() {
        return key;
    }

    @Override
    public String toString() {
        return "Latency{" +
                "key='" + key + '\'' +
                ", avgLatency=" + getAvg() +
                '}';
    }

}
