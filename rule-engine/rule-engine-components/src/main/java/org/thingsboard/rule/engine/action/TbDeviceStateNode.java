/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import org.thingsboard.server.queue.TbQueueMsgMetadata;

import java.util.Map;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "device state",
        nodeDescription = "Triggers device connectivity events",
        nodeDetails = "If incoming message originator is a device," +
                " registers configured event for that device in the Device State Service," +
                " which sends appropriate message to the Rule Engine. " +
                "Incoming message is forwarded using the <code>Success</code> chain," +
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

    private static final TbQueueCallback EMPTY_CALLBACK = new TbQueueCallback() {

        @Override
        public void onSuccess(TbQueueMsgMetadata metadata) {
        }

        @Override
        public void onFailure(Throwable t) {
        }

    };

    private final Map<TbMsgType, ConnectivityEvent> SUPPORTED_EVENTS = Map.of(
            TbMsgType.CONNECT_EVENT, this::sendDeviceConnectMsg,
            TbMsgType.ACTIVITY_EVENT, this::sendDeviceActivityMsg,
            TbMsgType.DISCONNECT_EVENT, this::sendDeviceDisconnectMsg,
            TbMsgType.INACTIVITY_EVENT, this::sendDeviceInactivityMsg
    );

    private interface ConnectivityEvent {

        void sendEvent(TbContext ctx, TbMsg msg);

    }

    private TbMsgType event;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        TbMsgType event = TbNodeUtils.convert(configuration, TbDeviceStateNodeConfiguration.class).getEvent();
        if (event == null) {
            throw new TbNodeException("Event cannot be null!", true);
        }
        if (!SUPPORTED_EVENTS.containsKey(event)) {
            throw new TbNodeException("Unsupported event: " + event, true);
        }
        this.event = event;

    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        var originator = msg.getOriginator();
        if (!ctx.isLocalEntity(originator)) {
            log.warn("[{}][device-state-node] Received message from non-local entity [{}]!", ctx.getSelfId(), originator);
            return;
        }
        if (!EntityType.DEVICE.equals(originator.getEntityType())) {
            ctx.tellSuccess(msg);
            return;
        }
        SUPPORTED_EVENTS.get(event).sendEvent(ctx, msg);
        ctx.tellSuccess(msg);
    }

    private void sendDeviceConnectMsg(TbContext ctx, TbMsg msg) {
        var tenantUuid = ctx.getTenantId().getId();
        var deviceUuid = msg.getOriginator().getId();
        var deviceConnectMsg = TransportProtos.DeviceConnectProto.newBuilder()
                .setTenantIdMSB(tenantUuid.getMostSignificantBits())
                .setTenantIdLSB(tenantUuid.getLeastSignificantBits())
                .setDeviceIdMSB(deviceUuid.getMostSignificantBits())
                .setDeviceIdLSB(deviceUuid.getLeastSignificantBits())
                .build();
        var toCoreMsg = TransportProtos.ToCoreMsg.newBuilder()
                .setDeviceConnectMsg(deviceConnectMsg)
                .build();
        ctx.getClusterService().pushMsgToCore(ctx.getTenantId(), msg.getOriginator(), toCoreMsg, EMPTY_CALLBACK);
    }

    private void sendDeviceActivityMsg(TbContext ctx, TbMsg msg) {
        var tenantUuid = ctx.getTenantId().getId();
        var deviceUuid = msg.getOriginator().getId();
        var deviceActivityMsg = TransportProtos.DeviceActivityProto.newBuilder()
                .setTenantIdMSB(tenantUuid.getMostSignificantBits())
                .setTenantIdLSB(tenantUuid.getLeastSignificantBits())
                .setDeviceIdMSB(deviceUuid.getMostSignificantBits())
                .setDeviceIdLSB(deviceUuid.getLeastSignificantBits())
                .setLastActivityTime(System.currentTimeMillis())
                .build();
        var toCoreMsg = TransportProtos.ToCoreMsg.newBuilder()
                .setDeviceActivityMsg(deviceActivityMsg)
                .build();
        ctx.getClusterService().pushMsgToCore(ctx.getTenantId(), msg.getOriginator(), toCoreMsg, EMPTY_CALLBACK);
    }

    private void sendDeviceDisconnectMsg(TbContext ctx, TbMsg msg) {
        var tenantUuid = ctx.getTenantId().getId();
        var deviceUuid = msg.getOriginator().getId();
        var deviceDisconnectMsg = TransportProtos.DeviceDisconnectProto.newBuilder()
                .setTenantIdMSB(tenantUuid.getMostSignificantBits())
                .setTenantIdLSB(tenantUuid.getLeastSignificantBits())
                .setDeviceIdMSB(deviceUuid.getMostSignificantBits())
                .setDeviceIdLSB(deviceUuid.getLeastSignificantBits())
                .build();
        var toCoreMsg = TransportProtos.ToCoreMsg.newBuilder()
                .setDeviceDisconnectMsg(deviceDisconnectMsg)
                .build();
        ctx.getClusterService().pushMsgToCore(ctx.getTenantId(), msg.getOriginator(), toCoreMsg, EMPTY_CALLBACK);
    }

    private void sendDeviceInactivityMsg(TbContext ctx, TbMsg msg) {
        var tenantUuid = ctx.getTenantId().getId();
        var deviceUuid = msg.getOriginator().getId();
        var deviceInactivityMsg = TransportProtos.DeviceInactivityProto.newBuilder()
                .setTenantIdMSB(tenantUuid.getMostSignificantBits())
                .setTenantIdLSB(tenantUuid.getLeastSignificantBits())
                .setDeviceIdMSB(deviceUuid.getMostSignificantBits())
                .setDeviceIdLSB(deviceUuid.getLeastSignificantBits())
                .build();
        var toCoreMsg = TransportProtos.ToCoreMsg.newBuilder()
                .setDeviceInactivityMsg(deviceInactivityMsg)
                .build();
        ctx.getClusterService().pushMsgToCore(ctx.getTenantId(), msg.getOriginator(), toCoreMsg, EMPTY_CALLBACK);
    }

}
