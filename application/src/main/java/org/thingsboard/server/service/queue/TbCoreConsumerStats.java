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
    public static final String DEVICE_CONNECTS = "deviceConnect";
    public static final String DEVICE_ACTIVITIES = "deviceActivity";
    public static final String DEVICE_DISCONNECTS = "deviceDisconnect";
    public static final String DEVICE_INACTIVITIES = "deviceInactivity";
    public static final String DEVICE_INACTIVITY_TIMEOUT_UPDATES = "deviceInactivityTimeoutUpdate";

    public static final String TO_CORE_NF_OTHER = "coreNfOther"; // normally, there is no messages when codebase is fine
    public static final String TO_CORE_NF_COMPONENT_LIFECYCLE = "coreNfCompLfcl";
    public static final String TO_CORE_NF_DEVICE_RPC_RESPONSE = "coreNfDevRpcRsp";
    public static final String TO_CORE_NF_NOTIFICATION_RULE_PROCESSOR = "coreNfNfRlProc";
    public static final String TO_CORE_NF_QUEUE_UPDATE = "coreNfQueueUpd";
    public static final String TO_CORE_NF_QUEUE_DELETE = "coreNfQueueDel";
    public static final String TO_CORE_NF_SUBSCRIPTION_SERVICE = "coreNfSubSvc";
    public static final String TO_CORE_NF_SUBSCRIPTION_MANAGER = "coreNfSubMgr";
    public static final String TO_CORE_NF_VC_RESPONSE = "coreNfVCRsp";

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
    private final StatsCounter deviceConnectsCounter;
    private final StatsCounter deviceActivitiesCounter;
    private final StatsCounter deviceDisconnectsCounter;
    private final StatsCounter deviceInactivitiesCounter;
    private final StatsCounter deviceInactivityTimeoutUpdatesCounter;

    private final StatsCounter toCoreNfOtherCounter;
    private final StatsCounter toCoreNfComponentLifecycleCounter;
    private final StatsCounter toCoreNfDeviceRpcResponseCounter;
    private final StatsCounter toCoreNfNotificationRuleProcessorCounter;
    private final StatsCounter toCoreNfQueueUpdateCounter;
    private final StatsCounter toCoreNfQueueDeleteCounter;
    private final StatsCounter toCoreNfSubscriptionServiceCounter;
    private final StatsCounter toCoreNfSubscriptionManagerCounter;
    private final StatsCounter toCoreNfVersionControlResponseCounter;

    private final List<StatsCounter> counters = new ArrayList<>(23);

    public TbCoreConsumerStats(StatsFactory statsFactory) {
        String statsKey = StatsType.CORE.getName();

        this.totalCounter = register(statsFactory.createStatsCounter(statsKey, TOTAL_MSGS));
        this.sessionEventCounter = register(statsFactory.createStatsCounter(statsKey, SESSION_EVENTS));
        this.getAttributesCounter = register(statsFactory.createStatsCounter(statsKey, GET_ATTRIBUTE));
        this.subscribeToAttributesCounter = register(statsFactory.createStatsCounter(statsKey, ATTRIBUTE_SUBSCRIBES));
        this.subscribeToRPCCounter = register(statsFactory.createStatsCounter(statsKey, RPC_SUBSCRIBES));
        this.toDeviceRPCCallResponseCounter = register(statsFactory.createStatsCounter(statsKey, TO_DEVICE_RPC_CALL_RESPONSES));
        this.subscriptionInfoCounter = register(statsFactory.createStatsCounter(statsKey, SUBSCRIPTION_INFO));
        this.claimDeviceCounter = register(statsFactory.createStatsCounter(statsKey, DEVICE_CLAIMS));
        this.deviceStateCounter = register(statsFactory.createStatsCounter(statsKey, DEVICE_STATES));
        this.subscriptionMsgCounter = register(statsFactory.createStatsCounter(statsKey, SUBSCRIPTION_MSGS));
        this.deviceConnectsCounter = register(statsFactory.createStatsCounter(statsKey, DEVICE_CONNECTS));
        this.deviceActivitiesCounter = register(statsFactory.createStatsCounter(statsKey, DEVICE_ACTIVITIES));
        this.deviceDisconnectsCounter = register(statsFactory.createStatsCounter(statsKey, DEVICE_DISCONNECTS));
        this.deviceInactivitiesCounter = register(statsFactory.createStatsCounter(statsKey, DEVICE_INACTIVITIES));
        this.deviceInactivityTimeoutUpdatesCounter = register(statsFactory.createStatsCounter(statsKey, DEVICE_INACTIVITY_TIMEOUT_UPDATES));

        // Core notification counters
        this.toCoreNfOtherCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_OTHER));
        this.toCoreNfComponentLifecycleCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_COMPONENT_LIFECYCLE));
        this.toCoreNfDeviceRpcResponseCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_DEVICE_RPC_RESPONSE));
        this.toCoreNfNotificationRuleProcessorCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_NOTIFICATION_RULE_PROCESSOR));
        this.toCoreNfQueueUpdateCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_QUEUE_UPDATE));
        this.toCoreNfQueueDeleteCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_QUEUE_DELETE));
        this.toCoreNfSubscriptionServiceCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_SUBSCRIPTION_SERVICE));
        this.toCoreNfSubscriptionManagerCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_SUBSCRIPTION_MANAGER));
        this.toCoreNfVersionControlResponseCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_VC_RESPONSE));
    }

    private StatsCounter register(StatsCounter counter) {
        counters.add(counter);
        return counter;
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

    public void log(TransportProtos.DeviceConnectProto msg) {
        totalCounter.increment();
        deviceConnectsCounter.increment();
    }

    public void log(TransportProtos.DeviceActivityProto msg) {
        totalCounter.increment();
        deviceActivitiesCounter.increment();
    }

    public void log(TransportProtos.DeviceDisconnectProto msg) {
        totalCounter.increment();
        deviceDisconnectsCounter.increment();
    }

    public void log(TransportProtos.DeviceInactivityProto msg) {
        totalCounter.increment();
        deviceInactivitiesCounter.increment();
    }

    public void log(TransportProtos.DeviceInactivityTimeoutUpdateProto msg) {
        totalCounter.increment();
        deviceInactivityTimeoutUpdatesCounter.increment();
    }

    public void log(TransportProtos.SubscriptionMgrMsgProto msg) {
        totalCounter.increment();
        subscriptionMsgCounter.increment();
    }

    public void log(TransportProtos.ToCoreNotificationMsg msg) {
        totalCounter.increment();
        if (msg.hasToLocalSubscriptionServiceMsg()) {
            toCoreNfSubscriptionServiceCounter.increment();
        } else if (msg.hasFromDeviceRpcResponse()) {
            toCoreNfDeviceRpcResponseCounter.increment();
        } else if (msg.hasComponentLifecycle()) {
            toCoreNfComponentLifecycleCounter.increment();
        } else if (msg.getQueueUpdateMsgsCount() > 0) {
            toCoreNfQueueUpdateCounter.increment();
        } else if (msg.getQueueDeleteMsgsCount() > 0) {
            toCoreNfQueueDeleteCounter.increment();
        } else if (msg.hasVcResponseMsg()) {
            toCoreNfVersionControlResponseCounter.increment();
        } else if (msg.hasToSubscriptionMgrMsg()) {
            toCoreNfSubscriptionManagerCounter.increment();
        } else if (msg.hasNotificationRuleProcessorMsg()) {
            toCoreNfNotificationRuleProcessorCounter.increment();
        } else {
            toCoreNfOtherCounter.increment();
        }
    }

    public void printStats() {
        int total = totalCounter.get();
        if (total > 0) {
            StringBuilder stats = new StringBuilder();
            counters.forEach(counter ->
                    stats.append(counter.getName()).append(" = [").append(counter.get()).append("] "));
            log.info("Core Stats: {}", stats);
        }
    }

    public void reset() {
        counters.forEach(StatsCounter::clear);
    }

}
