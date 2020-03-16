/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.queue;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class TbRuleEngineConsumerStats {

    private final AtomicInteger totalCounter = new AtomicInteger(0);
    private final AtomicInteger postTelemetryCounter = new AtomicInteger(0);
    private final AtomicInteger postAttributesCounter = new AtomicInteger(0);
    private final AtomicInteger toServerRPCCallRequestCounter = new AtomicInteger(0);

    public void log(TransportProtos.TransportToRuleEngineMsg msg) {
        totalCounter.incrementAndGet();
        if (msg.hasPostTelemetry()) {
            postTelemetryCounter.incrementAndGet();
        }
        if (msg.hasPostAttributes()) {
            postAttributesCounter.incrementAndGet();
        }
        if (msg.hasToServerRPCCallRequest()) {
            toServerRPCCallRequestCounter.incrementAndGet();
        }
    }

    public void printStats() {
        int total = totalCounter.getAndSet(0);
        if (total > 0) {
            log.info("Transport total [{}] telemetry [{}] attributes [{}] toServerRpc [{}]",
                    total, postTelemetryCounter.getAndSet(0),
                    postAttributesCounter.getAndSet(0), toServerRPCCallRequestCounter.getAndSet(0));
        }
    }
}
