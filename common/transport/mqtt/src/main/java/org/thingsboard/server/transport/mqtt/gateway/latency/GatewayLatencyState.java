/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.transport.mqtt.gateway.latency;

import lombok.Getter;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GatewayLatencyState {

    private final Map<String, ConnectorLatencyState> connectors;
    private final Lock updateLock;

    @Getter
    private volatile TransportProtos.SessionInfoProto sessionInfo;

    public GatewayLatencyState(TransportProtos.SessionInfoProto sessionInfo) {
        this.connectors = new HashMap<>();
        this.updateLock = new ReentrantLock();
        this.sessionInfo = sessionInfo;
    }

    public void updateSessionInfo(TransportProtos.SessionInfoProto sessionInfo) {
        this.sessionInfo = sessionInfo;
    }

    public void update(long ts, Map<String, GatewayLatencyData> latencyData) {
        updateLock.lock();
        try {
            latencyData.forEach((connectorName, data) -> {
                connectors.computeIfAbsent(connectorName, k -> new ConnectorLatencyState()).update(ts, data);
            });
        } finally {
            updateLock.unlock();
        }
    }

    public void clear() {
        connectors.clear();
    }

    public Map<String, ConnectorLatencyResult> getLatencyStateResult() {
        Map<String, ConnectorLatencyResult> result = new HashMap<>();
        connectors.forEach((name, state) -> result.put(name, state.getResult()));
        return result;
    }

    public boolean isEmpty() {
        return connectors.isEmpty();
    }

    private static class ConnectorLatencyState {
        private final AtomicInteger count;
        private final AtomicLong gwLatencySum;
        private final AtomicLong transportLatencySum;
        private volatile long minGwLatency;
        private volatile long maxGwLatency;
        private volatile long minTransportLatency;
        private volatile long maxTransportLatency;

        private ConnectorLatencyState() {
            this.count = new AtomicInteger(0);
            this.gwLatencySum = new AtomicLong(0);
            this.transportLatencySum = new AtomicLong(0);
        }

        private void update(long serverReceiveTs, GatewayLatencyData latencyData) {
            long gwLatency = latencyData.publishedTs() - latencyData.receivedTs();
            long transportLatency = serverReceiveTs - latencyData.publishedTs();
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

        private ConnectorLatencyResult getResult() {
            long count = this.count.get();
            long avgGwLatency = gwLatencySum.get() / count;
            long transportLatencyAvg = transportLatencySum.get() / count;
            return new ConnectorLatencyResult(avgGwLatency, minGwLatency, maxGwLatency, transportLatencyAvg, minTransportLatency, maxTransportLatency);
        }
    }

    public record ConnectorLatencyResult(long avgGwLatency, long minGwLatency, long maxGwLatency,
                                         long transportLatencyAvg, long minTransportLatency, long maxTransportLatency) {
    }

}
