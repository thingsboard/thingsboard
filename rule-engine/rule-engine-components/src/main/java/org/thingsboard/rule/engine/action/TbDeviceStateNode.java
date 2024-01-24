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

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.common.SimpleTbQueueCallback;

import java.util.EnumSet;
import java.util.Set;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "device state",
        nodeDescription = "Triggers device connectivity events",
        nodeDetails = "If incoming message originator is a device," +
                " registers configured event for that device in the Device State Service," +
                " which sends appropriate message to the Rule Engine." +
                " If metadata <code>ts</code> property is present, it will be used as event timestamp." +
                " Incoming message is forwarded using the <code>Success</code> chain," +
                " unless an unexpected error occurs during message processing" +
                " then incoming message is forwarded using the <code>Failure</code> chain." +
                "<br>" +
                "Supported device connectivity events are:" +
                "<ul>" +
                "<li>Connect event</li>" +
                "<li>Disconnect event</li>" +
                "<li>Activity event</li>" +
                "<li>Inactivity event</li>" +
                "</ul>" +
                "This node is particularly useful when device isn't using transports to receive data," +
                " such as when fetching data from external API or computing new data within the rule chain.",
        configClazz = TbDeviceStateNodeConfiguration.class,
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeDeviceStateConfig"
)
public class TbDeviceStateNode implements TbNode {

    static final Set<TbMsgType> SUPPORTED_EVENTS = EnumSet.of(
            TbMsgType.CONNECT_EVENT, TbMsgType.ACTIVITY_EVENT, TbMsgType.DISCONNECT_EVENT, TbMsgType.INACTIVITY_EVENT
    );

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
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        var originator = msg.getOriginator();
        if (!ctx.isLocalEntity(originator)) {
            log.warn("[{}][{}] Received message from non-local entity [{}]!",
                    ctx.getTenantId().getId(), ctx.getSelfId().getId(), originator.getId());
            ctx.ack(msg);
            return;
        }
        if (!EntityType.DEVICE.equals(originator.getEntityType())) {
            ctx.tellSuccess(msg);
            return;
        }
        switch (event) {
            case CONNECT_EVENT:
                sendDeviceConnectMsg(ctx, msg);
                break;
            case ACTIVITY_EVENT:
                sendDeviceActivityMsg(ctx, msg);
                break;
            case DISCONNECT_EVENT:
                sendDeviceDisconnectMsg(ctx, msg);
                break;
            case INACTIVITY_EVENT:
                sendDeviceInactivityMsg(ctx, msg);
                break;
            default:
                ctx.tellFailure(msg, new IllegalStateException("Configured event [" + event + "] is not supported!"));
        }
    }

    private void sendDeviceConnectMsg(TbContext ctx, TbMsg msg) {
        var tenantUuid = ctx.getTenantId().getId();
        var deviceUuid = msg.getOriginator().getId();
        var deviceConnectMsg = TransportProtos.DeviceConnectProto.newBuilder()
                .setTenantIdMSB(tenantUuid.getMostSignificantBits())
                .setTenantIdLSB(tenantUuid.getLeastSignificantBits())
                .setDeviceIdMSB(deviceUuid.getMostSignificantBits())
                .setDeviceIdLSB(deviceUuid.getLeastSignificantBits())
                .setLastConnectTime(msg.getMetaDataTs())
                .build();
        var toCoreMsg = TransportProtos.ToCoreMsg.newBuilder()
                .setDeviceConnectMsg(deviceConnectMsg)
                .build();
        ctx.getClusterService().pushMsgToCore(
                ctx.getTenantId(), msg.getOriginator(), toCoreMsg, getMsgEnqueuedCallback(ctx, msg)
        );
    }

    private void sendDeviceActivityMsg(TbContext ctx, TbMsg msg) {
        var tenantUuid = ctx.getTenantId().getId();
        var deviceUuid = msg.getOriginator().getId();
        var deviceActivityMsg = TransportProtos.DeviceActivityProto.newBuilder()
                .setTenantIdMSB(tenantUuid.getMostSignificantBits())
                .setTenantIdLSB(tenantUuid.getLeastSignificantBits())
                .setDeviceIdMSB(deviceUuid.getMostSignificantBits())
                .setDeviceIdLSB(deviceUuid.getLeastSignificantBits())
                .setLastActivityTime(msg.getMetaDataTs())
                .build();
        var toCoreMsg = TransportProtos.ToCoreMsg.newBuilder()
                .setDeviceActivityMsg(deviceActivityMsg)
                .build();
        ctx.getClusterService().pushMsgToCore(
                ctx.getTenantId(), msg.getOriginator(), toCoreMsg, getMsgEnqueuedCallback(ctx, msg)
        );
    }

    private void sendDeviceDisconnectMsg(TbContext ctx, TbMsg msg) {
        var tenantUuid = ctx.getTenantId().getId();
        var deviceUuid = msg.getOriginator().getId();
        var deviceDisconnectMsg = TransportProtos.DeviceDisconnectProto.newBuilder()
                .setTenantIdMSB(tenantUuid.getMostSignificantBits())
                .setTenantIdLSB(tenantUuid.getLeastSignificantBits())
                .setDeviceIdMSB(deviceUuid.getMostSignificantBits())
                .setDeviceIdLSB(deviceUuid.getLeastSignificantBits())
                .setLastDisconnectTime(msg.getMetaDataTs())
                .build();
        var toCoreMsg = TransportProtos.ToCoreMsg.newBuilder()
                .setDeviceDisconnectMsg(deviceDisconnectMsg)
                .build();
        ctx.getClusterService().pushMsgToCore(
                ctx.getTenantId(), msg.getOriginator(), toCoreMsg, getMsgEnqueuedCallback(ctx, msg)
        );
    }

    private void sendDeviceInactivityMsg(TbContext ctx, TbMsg msg) {
        var tenantUuid = ctx.getTenantId().getId();
        var deviceUuid = msg.getOriginator().getId();
        var deviceInactivityMsg = TransportProtos.DeviceInactivityProto.newBuilder()
                .setTenantIdMSB(tenantUuid.getMostSignificantBits())
                .setTenantIdLSB(tenantUuid.getLeastSignificantBits())
                .setDeviceIdMSB(deviceUuid.getMostSignificantBits())
                .setDeviceIdLSB(deviceUuid.getLeastSignificantBits())
                .setLastInactivityTime(msg.getMetaDataTs())
                .build();
        var toCoreMsg = TransportProtos.ToCoreMsg.newBuilder()
                .setDeviceInactivityMsg(deviceInactivityMsg)
                .build();
        ctx.getClusterService().pushMsgToCore(
                ctx.getTenantId(), msg.getOriginator(), toCoreMsg, getMsgEnqueuedCallback(ctx, msg)
        );
    }

    private TbQueueCallback getMsgEnqueuedCallback(TbContext ctx, TbMsg msg) {
        return new SimpleTbQueueCallback(metadata -> ctx.tellSuccess(msg), t -> ctx.tellFailure(msg, t));
    }

}
