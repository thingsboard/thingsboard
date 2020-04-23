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

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class TbCoreConsumerStats {

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
        int total = totalCounter.getAndSet(0);
        if (total > 0) {
            log.info("Total [{}] sessionEvents [{}] getAttr [{}] subToAttr [{}] subToRpc [{}] toDevRpc [{}] subInfo [{}] claimDevice [{}]" +
                            " deviceState [{}] subMgr [{}] coreNfs [{}]",
                    total, sessionEventCounter.getAndSet(0),
                    getAttributesCounter.getAndSet(0), subscribeToAttributesCounter.getAndSet(0),
                    subscribeToRPCCounter.getAndSet(0), toDeviceRPCCallResponseCounter.getAndSet(0),
                    subscriptionInfoCounter.getAndSet(0), claimDeviceCounter.getAndSet(0)
                    , deviceStateCounter.getAndSet(0), subscriptionMsgCounter.getAndSet(0), toCoreNotificationsCounter.getAndSet(0));
        }
    }

}
