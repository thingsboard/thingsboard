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
package org.thingsboard.server.service.transport;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class RuleEngineStats {

    private final AtomicInteger totalCounter = new AtomicInteger(0);
    private final AtomicInteger sessionEventCounter = new AtomicInteger(0);
    private final AtomicInteger postTelemetryCounter = new AtomicInteger(0);
    private final AtomicInteger postAttributesCounter = new AtomicInteger(0);
    private final AtomicInteger getAttributesCounter = new AtomicInteger(0);
    private final AtomicInteger subscribeToAttributesCounter = new AtomicInteger(0);
    private final AtomicInteger subscribeToRPCCounter = new AtomicInteger(0);
    private final AtomicInteger toDeviceRPCCallResponseCounter = new AtomicInteger(0);
    private final AtomicInteger toServerRPCCallRequestCounter = new AtomicInteger(0);
    private final AtomicInteger subscriptionInfoCounter = new AtomicInteger(0);
    private final AtomicInteger claimDeviceCounter = new AtomicInteger(0);

    public void log(TransportProtos.TransportToDeviceActorMsg msg) {
        totalCounter.incrementAndGet();
        if (msg.hasSessionEvent()) {
            sessionEventCounter.incrementAndGet();
        }
        if (msg.hasPostTelemetry()) {
            postTelemetryCounter.incrementAndGet();
        }
        if (msg.hasPostAttributes()) {
            postAttributesCounter.incrementAndGet();
        }
        if (msg.hasGetAttributes()) {
            getAttributesCounter.incrementAndGet();
        }
        if (msg.hasSubscribeToAttributes()) {
            subscribeToAttributesCounter.incrementAndGet();
        }
        if (msg.hasSubscribeToRPC()) {
            subscribeToRPCCounter.incrementAndGet();
        }
        if (msg.hasToDeviceRPCCallResponse()) {
            toDeviceRPCCallResponseCounter.incrementAndGet();
        }
        if (msg.hasToServerRPCCallRequest()) {
            toServerRPCCallRequestCounter.incrementAndGet();
        }
        if (msg.hasSubscriptionInfo()) {
            subscriptionInfoCounter.incrementAndGet();
        }
        if (msg.hasClaimDevice()) {
            claimDeviceCounter.incrementAndGet();
        }
    }

    public void printStats() {
        int total = totalCounter.getAndSet(0);
        if (total > 0) {
            log.info("Transport total [{}] sessionEvents [{}] telemetry [{}] attributes [{}] getAttr [{}] subToAttr [{}] subToRpc [{}] toDevRpc [{}] " +
                            "toServerRpc [{}] subInfo [{}] claimDevice [{}] ",
                    total, sessionEventCounter.getAndSet(0), postTelemetryCounter.getAndSet(0),
                    postAttributesCounter.getAndSet(0), getAttributesCounter.getAndSet(0), subscribeToAttributesCounter.getAndSet(0),
                    subscribeToRPCCounter.getAndSet(0), toDeviceRPCCallResponseCounter.getAndSet(0),
                    toServerRPCCallRequestCounter.getAndSet(0), subscriptionInfoCounter.getAndSet(0), claimDeviceCounter.getAndSet(0));
        }
    }
}
