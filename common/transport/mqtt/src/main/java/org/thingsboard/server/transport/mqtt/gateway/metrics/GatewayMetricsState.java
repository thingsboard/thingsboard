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
package org.thingsboard.server.transport.mqtt.gateway.metrics;

import lombok.Getter;
import org.thingsboard.server.common.msg.gateway.metrics.GatewayMetadata;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GatewayMetricsState {

    private final Map<String, ConnectorMetricsState> connectors;
    private final Lock updateLock;

    @Getter
    private volatile TransportProtos.SessionInfoProto sessionInfo;

    public GatewayMetricsState(TransportProtos.SessionInfoProto sessionInfo) {
        this.connectors = new HashMap<>();
        this.updateLock = new ReentrantLock();
        this.sessionInfo = sessionInfo;
    }

    public void updateSessionInfo(TransportProtos.SessionInfoProto sessionInfo) {
        this.sessionInfo = sessionInfo;
    }

    public void update(List<GatewayMetadata> metricsData, long serverReceiveTs) {
        updateLock.lock();
        try {
            metricsData.forEach(data -> {
                connectors.computeIfAbsent(data.connector(), k -> new ConnectorMetricsState()).update(data, serverReceiveTs);
            });
        } finally {
            updateLock.unlock();
        }
    }

    public Map<String, ConnectorMetricsResult> getStateResult() {
        Map<String, ConnectorMetricsResult> result = new HashMap<>();
        updateLock.lock();
        try {
            connectors.forEach((name, state) -> result.put(name, state.getResult()));
            connectors.clear();
        } finally {
            updateLock.unlock();
        }

        return result;
    }

    public boolean isEmpty() {
        return connectors.isEmpty();
    }

    private static class ConnectorMetricsState {
        private final AtomicInteger count;
        private final AtomicLong gwLatencySum;
        private final AtomicLong transportLatencySum;
        private volatile long minGwLatency;
        private volatile long maxGwLatency;
        private volatile long minTransportLatency;
        private volatile long maxTransportLatency;

        private ConnectorMetricsState() {
            this.count = new AtomicInteger(0);
            this.gwLatencySum = new AtomicLong(0);
            this.transportLatencySum = new AtomicLong(0);
        }

        private void update(GatewayMetadata metricsData, long serverReceiveTs) {
            long gwLatency = metricsData.publishedTs() - metricsData.receivedTs();
            long transportLatency = serverReceiveTs - metricsData.publishedTs();
            count.incrementAndGet();
            gwLatencySum.addAndGet(gwLatency);
            transportLatencySum.addAndGet(transportLatency);
            if (minGwLatency == 0 || minGwLatency > gwLatency) {
                minGwLatency = gwLatency;
            }
            if (maxGwLatency < gwLatency) {
                maxGwLatency = gwLatency;
            }
            if (minTransportLatency == 0 || minTransportLatency > transportLatency) {
                minTransportLatency = transportLatency;
            }
            if (maxTransportLatency < transportLatency) {
                maxTransportLatency = transportLatency;
            }
        }

        private ConnectorMetricsResult getResult() {
            long count = this.count.get();
            long avgGwLatency = gwLatencySum.get() / count;
            long avgTransportLatency = transportLatencySum.get() / count;
            return new ConnectorMetricsResult(avgGwLatency, minGwLatency, maxGwLatency, avgTransportLatency, minTransportLatency, maxTransportLatency);
        }
    }

    public record ConnectorMetricsResult(long avgGwLatency, long minGwLatency, long maxGwLatency,
                                         long avgTransportLatency, long minTransportLatency, long maxTransportLatency) {
    }

}
