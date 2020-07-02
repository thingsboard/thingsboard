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
package org.thingsboard.server.service.queue;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class TbCoreConsumerStats {
    public static final String TOTAL_MSGS = "totalMsgs";
    public static final String SESSION_EVENTS = "sessionEvents";
    public static final String GET_ATTRIBUTE = "getAttr";
    public static final String ATTRIBUTE_SUBSCRIBES = "subToAttr";
    public static final String RPC_SUBSCRIBES = "subToRpc";
    public static final String TO_DEVICE_RPC_CALL_RESPONSES = "toDevRpc";
    public static final String SUBSCRIPTION_INFO = "subInfo";
    public static final String DEVICE_CLAIMS = "claimDevice";
    public static final String DEVICE_STATES = "deviceState";
    public static final String SUBSCRIPTION_MSGS = "subMsgs";
    public static final String TO_CORE_NOTIFICATIONS = "coreNfs";

    private final AtomicInteger totalCounter = new AtomicInteger(0);
    private final AtomicInteger sessionEventCounter = new AtomicInteger(0);
    private final AtomicInteger getAttributesCounter = new AtomicInteger(0);
    private final AtomicInteger subscribeToAttributesCounter = new AtomicInteger(0);
    private final AtomicInteger subscribeToRPCCounter = new AtomicInteger(0);
    private final AtomicInteger toDeviceRPCCallResponseCounter = new AtomicInteger(0);
    private final AtomicInteger subscriptionInfoCounter = new AtomicInteger(0);
    private final AtomicInteger claimDeviceCounter = new AtomicInteger(0);

    private final AtomicInteger deviceStateCounter = new AtomicInteger(0);
    private final AtomicInteger subscriptionMsgCounter = new AtomicInteger(0);
    private final AtomicInteger toCoreNotificationsCounter = new AtomicInteger(0);

    private final Map<String, AtomicInteger> counters;

    public TbCoreConsumerStats() {
        Map<String, AtomicInteger> tmpCounters = new HashMap<>();

        tmpCounters.put(TOTAL_MSGS, totalCounter);
        tmpCounters.put(SESSION_EVENTS, sessionEventCounter);
        tmpCounters.put(GET_ATTRIBUTE, getAttributesCounter);
        tmpCounters.put(ATTRIBUTE_SUBSCRIBES, subscribeToAttributesCounter);
        tmpCounters.put(RPC_SUBSCRIBES, subscribeToRPCCounter);
        tmpCounters.put(TO_DEVICE_RPC_CALL_RESPONSES, toDeviceRPCCallResponseCounter);
        tmpCounters.put(SUBSCRIPTION_INFO, subscriptionInfoCounter);
        tmpCounters.put(DEVICE_CLAIMS, claimDeviceCounter);

        tmpCounters.put(DEVICE_STATES, deviceStateCounter);
        tmpCounters.put(SUBSCRIPTION_MSGS, subscriptionMsgCounter);
        tmpCounters.put(TO_CORE_NOTIFICATIONS, toCoreNotificationsCounter);

        counters = Collections.unmodifiableMap(tmpCounters);
    }

    public Map<String, AtomicInteger> getCounters() {
        return counters;
    }

    public void log(TransportProtos.TransportToDeviceActorMsg msg) {
        totalCounter.incrementAndGet();
        if (msg.hasSessionEvent()) {
            sessionEventCounter.incrementAndGet();
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
        if (msg.hasSubscriptionInfo()) {
            subscriptionInfoCounter.incrementAndGet();
        }
        if (msg.hasClaimDevice()) {
            claimDeviceCounter.incrementAndGet();
        }
    }

    public void log(TransportProtos.DeviceStateServiceMsgProto msg) {
        totalCounter.incrementAndGet();
        deviceStateCounter.incrementAndGet();
    }

    public void log(TransportProtos.SubscriptionMgrMsgProto msg) {
        totalCounter.incrementAndGet();
        subscriptionMsgCounter.incrementAndGet();
    }

    public void log(TransportProtos.ToCoreNotificationMsg msg) {
        totalCounter.incrementAndGet();
        toCoreNotificationsCounter.incrementAndGet();
    }

    public void printStats() {
        int total = totalCounter.get();
        if (total > 0) {
            StringBuilder stats = new StringBuilder();
            counters.forEach((label, value) -> {
                stats.append(label).append(" = [").append(value.get()).append("] ");
            });
            log.info("Core Stats: {}", stats);
        }
    }

    public void reset() {
        counters.values().forEach(counter -> counter.set(0));
    }
}
