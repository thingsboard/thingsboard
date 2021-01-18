/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
import org.thingsboard.server.common.stats.StatsCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.ArrayList;
import java.util.List;

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
    public static final String EDGE_NOTIFICATIONS = "edgeNfs";

    private final StatsCounter totalCounter;
    private final StatsCounter sessionEventCounter;
    private final StatsCounter getAttributesCounter;
    private final StatsCounter subscribeToAttributesCounter;
    private final StatsCounter subscribeToRPCCounter;
    private final StatsCounter toDeviceRPCCallResponseCounter;
    private final StatsCounter subscriptionInfoCounter;
    private final StatsCounter claimDeviceCounter;

    private final StatsCounter deviceStateCounter;
    private final StatsCounter subscriptionMsgCounter;
    private final StatsCounter toCoreNotificationsCounter;
    private final StatsCounter edgeNotificationsCounter;

    private final List<StatsCounter> counters = new ArrayList<>();

    public TbCoreConsumerStats(StatsFactory statsFactory) {
        String statsKey = StatsType.CORE.getName();

        this.totalCounter = statsFactory.createStatsCounter(statsKey, TOTAL_MSGS);
        this.sessionEventCounter = statsFactory.createStatsCounter(statsKey, SESSION_EVENTS);
        this.getAttributesCounter = statsFactory.createStatsCounter(statsKey, GET_ATTRIBUTE);
        this.subscribeToAttributesCounter = statsFactory.createStatsCounter(statsKey, ATTRIBUTE_SUBSCRIBES);
        this.subscribeToRPCCounter = statsFactory.createStatsCounter(statsKey, RPC_SUBSCRIBES);
        this.toDeviceRPCCallResponseCounter = statsFactory.createStatsCounter(statsKey, TO_DEVICE_RPC_CALL_RESPONSES);
        this.subscriptionInfoCounter = statsFactory.createStatsCounter(statsKey, SUBSCRIPTION_INFO);
        this.claimDeviceCounter = statsFactory.createStatsCounter(statsKey, DEVICE_CLAIMS);
        this.deviceStateCounter = statsFactory.createStatsCounter(statsKey, DEVICE_STATES);
        this.subscriptionMsgCounter = statsFactory.createStatsCounter(statsKey, SUBSCRIPTION_MSGS);
        this.toCoreNotificationsCounter = statsFactory.createStatsCounter(statsKey, TO_CORE_NOTIFICATIONS);
        this.edgeNotificationsCounter = statsFactory.createStatsCounter(statsKey, EDGE_NOTIFICATIONS);

        counters.add(totalCounter);
        counters.add(sessionEventCounter);
        counters.add(getAttributesCounter);
        counters.add(subscribeToAttributesCounter);
        counters.add(subscribeToRPCCounter);
        counters.add(toDeviceRPCCallResponseCounter);
        counters.add(subscriptionInfoCounter);
        counters.add(claimDeviceCounter);

        counters.add(deviceStateCounter);
        counters.add(subscriptionMsgCounter);
        counters.add(toCoreNotificationsCounter);
        counters.add(edgeNotificationsCounter);
    }

    public void log(TransportProtos.TransportToDeviceActorMsg msg) {
        totalCounter.increment();
        if (msg.hasSessionEvent()) {
            sessionEventCounter.increment();
        }
        if (msg.hasGetAttributes()) {
            getAttributesCounter.increment();
        }
        if (msg.hasSubscribeToAttributes()) {
            subscribeToAttributesCounter.increment();
        }
        if (msg.hasSubscribeToRPC()) {
            subscribeToRPCCounter.increment();
        }
        if (msg.hasToDeviceRPCCallResponse()) {
            toDeviceRPCCallResponseCounter.increment();
        }
        if (msg.hasSubscriptionInfo()) {
            subscriptionInfoCounter.increment();
        }
        if (msg.hasClaimDevice()) {
            claimDeviceCounter.increment();
        }
    }

    public void log(TransportProtos.DeviceStateServiceMsgProto msg) {
        totalCounter.increment();
        deviceStateCounter.increment();
    }

    public void log(TransportProtos.EdgeNotificationMsgProto msg) {
        totalCounter.increment();
        edgeNotificationsCounter.increment();
    }

    public void log(TransportProtos.SubscriptionMgrMsgProto msg) {
        totalCounter.increment();
        subscriptionMsgCounter.increment();
    }

    public void log(TransportProtos.ToCoreNotificationMsg msg) {
        totalCounter.increment();
        toCoreNotificationsCounter.increment();
    }

    public void printStats() {
        int total = totalCounter.get();
        if (total > 0) {
            StringBuilder stats = new StringBuilder();
            counters.forEach(counter -> {
                stats.append(counter.getName()).append(" = [").append(counter.get()).append("] ");
            });
            log.info("Core Stats: {}", stats);
        }
    }

    public void reset() {
        counters.forEach(StatsCounter::clear);
    }
}
