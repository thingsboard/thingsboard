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
package org.thingsboard.rule.engine.action;

import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleEngineDeviceStateManager;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;
import org.thingsboard.server.common.msg.queue.TbCallback;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "device state",
        nodeDescription = "Triggers device connectivity events",
        nodeDetails = "If incoming message originator is a device, registers configured event for that device in the Device State Service, which sends appropriate message to the Rule Engine." +
                " If metadata <code>ts</code> property is present, it will be used as event timestamp. Otherwise, the message timestamp will be used." +
                " Incoming message is forwarded using the <code>Success</code> chain, unless an unexpected error occurs during message processing" +
                " then incoming message is forwarded using the <code>Failure</code> chain." +
                "<br>" +
                "Supported device connectivity events are:" +
                "<ul>" +
                "<li>Connect event</li>" +
                "<li>Disconnect event</li>" +
                "<li>Activity event</li>" +
                "<li>Inactivity event</li>" +
                "</ul>" +
                "This node is particularly useful when device isn't using transports to receive data, such as when fetching data from external API or computing new data within the rule chain.",
        configClazz = TbDeviceStateNodeConfiguration.class,
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeDeviceStateConfig"
)
public class TbDeviceStateNode implements TbNode {

    private static final Set<TbMsgType> SUPPORTED_EVENTS = EnumSet.of(
            TbMsgType.CONNECT_EVENT, TbMsgType.ACTIVITY_EVENT, TbMsgType.DISCONNECT_EVENT, TbMsgType.INACTIVITY_EVENT
    );
    private static final Duration ONE_SECOND = Duration.ofSeconds(1L);
    private static final Duration ENTRY_EXPIRATION_TIME = Duration.ofDays(1L);
    private static final Duration ENTRY_CLEANUP_PERIOD = Duration.ofHours(1L);

    private Stopwatch stopwatch;
    private ConcurrentMap<DeviceId, Duration> lastActivityEventTimestamps;
    private TbMsgType event;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        TbMsgType event = TbNodeUtils.convert(configuration, TbDeviceStateNodeConfiguration.class).getEvent();
        if (event == null) {
            throw new TbNodeException("Event cannot be null!", true);
        }
        if (!SUPPORTED_EVENTS.contains(event)) {
            throw new TbNodeException("Unsupported event: " + event, true);
        }
        this.event = event;
        lastActivityEventTimestamps = new ConcurrentHashMap<>();
        stopwatch = Stopwatch.createStarted();
        scheduleCleanupMsg(ctx);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (msg.isTypeOf(TbMsgType.DEVICE_STATE_STALE_ENTRIES_CLEANUP_SELF_MSG)) {
            removeStaleEntries();
            scheduleCleanupMsg(ctx);
            return;
        }

        EntityType originatorEntityType = msg.getOriginator().getEntityType();
        if (!EntityType.DEVICE.equals(originatorEntityType)) {
            ctx.tellFailure(msg, new IllegalArgumentException(
                    "Unsupported originator entity type: [" + originatorEntityType + "]. Only DEVICE entity type is supported."
            ));
            return;
        }

        DeviceId originator = new DeviceId(msg.getOriginator().getId());

        lastActivityEventTimestamps.compute(originator, (__, lastEventTs) -> {
            Duration now = stopwatch.elapsed();

            if (lastEventTs == null) {
                sendEventAndTell(ctx, originator, msg);
                return now;
            }

            Duration elapsedSinceLastEventSent = now.minus(lastEventTs);
            if (elapsedSinceLastEventSent.compareTo(ONE_SECOND) < 0) {
                ctx.tellSuccess(msg);
                return lastEventTs;
            }

            sendEventAndTell(ctx, originator, msg);
            return now;
        });
    }

    private void scheduleCleanupMsg(TbContext ctx) {
        TbMsg cleanupMsg = ctx.newMsg(
                null, TbMsgType.DEVICE_STATE_STALE_ENTRIES_CLEANUP_SELF_MSG, ctx.getSelfId(), TbMsgMetaData.EMPTY, TbMsg.EMPTY_STRING
        );
        ctx.tellSelf(cleanupMsg, ENTRY_CLEANUP_PERIOD.toMillis());
    }

    private void removeStaleEntries() {
        lastActivityEventTimestamps.entrySet().removeIf(entry -> {
            Duration now = stopwatch.elapsed();
            Duration lastEventTs = entry.getValue();
            Duration elapsedSinceLastEventSent = now.minus(lastEventTs);
            return elapsedSinceLastEventSent.compareTo(ENTRY_EXPIRATION_TIME) > 0;
        });
    }

    private void sendEventAndTell(TbContext ctx, DeviceId originator, TbMsg msg) {
        TenantId tenantId = ctx.getTenantId();
        long eventTs = msg.getMetaDataTs();

        RuleEngineDeviceStateManager deviceStateManager = ctx.getDeviceStateManager();
        TbCallback callback = getMsgEnqueuedCallback(ctx, msg);

        switch (event) {
            case CONNECT_EVENT:
                deviceStateManager.onDeviceConnect(tenantId, originator, eventTs, callback);
                break;
            case ACTIVITY_EVENT:
                deviceStateManager.onDeviceActivity(tenantId, originator, eventTs, callback);
                break;
            case DISCONNECT_EVENT:
                deviceStateManager.onDeviceDisconnect(tenantId, originator, eventTs, callback);
                break;
            case INACTIVITY_EVENT:
                deviceStateManager.onDeviceInactivity(tenantId, originator, eventTs, callback);
                break;
            default:
                ctx.tellFailure(msg, new IllegalStateException("Configured event [" + event + "] is not supported!"));
        }
    }

    private TbCallback getMsgEnqueuedCallback(TbContext ctx, TbMsg msg) {
        return new TbCallback() {
            @Override
            public void onSuccess() {
                ctx.tellSuccess(msg);
            }

            @Override
            public void onFailure(Throwable t) {
                ctx.tellFailure(msg, t);
            }
        };
    }

    @Override
    public void onPartitionChangeMsg(TbContext ctx, PartitionChangeMsg msg) {
        lastActivityEventTimestamps.entrySet().removeIf(entry -> !ctx.isLocalEntity(entry.getKey()));
    }

    @Override
    public void destroy() {
        if (lastActivityEventTimestamps != null) {
            lastActivityEventTimestamps.clear();
            lastActivityEventTimestamps = null;
        }
        stopwatch = null;
        event = null;
    }

}
