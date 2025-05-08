/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.actors.device;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.TbActorException;
import org.thingsboard.server.actors.service.ContextAwareActor;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponseActorMsg;
import org.thingsboard.server.common.msg.rpc.RemoveRpcActorMsg;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequestActorMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceAttributesEventNotificationMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceEdgeUpdateMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceNameOrTypeUpdateMsg;
import org.thingsboard.server.common.msg.timeout.DeviceActorServerSideRpcTimeoutMsg;
import org.thingsboard.server.service.transport.msg.TransportToDeviceActorMsgWrapper;

@Slf4j
public class DeviceActor extends ContextAwareActor {

    private final DeviceActorMessageProcessor processor;

    DeviceActor(ActorSystemContext systemContext, TenantId tenantId, DeviceId deviceId) {
        super(systemContext);
        this.processor = new DeviceActorMessageProcessor(systemContext, tenantId, deviceId);
    }

    @Override
    public void init(TbActorCtx ctx) throws TbActorException {
        super.init(ctx);
        log.debug("[{}][{}] Starting device actor.", processor.tenantId, processor.deviceId);
        try {
            processor.init(ctx);
            log.debug("[{}][{}] Device actor started.", processor.tenantId, processor.deviceId);
        } catch (Exception e) {
            log.warn("[{}][{}] Unknown failure", processor.tenantId, processor.deviceId, e);
            throw new TbActorException("Failed to initialize device actor", e);
        }
    }

    @Override
    protected boolean doProcess(TbActorMsg msg) {
        switch (msg.getMsgType()) {
            case TRANSPORT_TO_DEVICE_ACTOR_MSG:
                processor.process((TransportToDeviceActorMsgWrapper) msg);
                break;
            case DEVICE_ATTRIBUTES_UPDATE_TO_DEVICE_ACTOR_MSG:
                processor.processAttributesUpdate((DeviceAttributesEventNotificationMsg) msg);
                break;
            case DEVICE_DELETE_TO_DEVICE_ACTOR_MSG:
                ctx.stop(ctx.getSelf());
                break;
            case DEVICE_CREDENTIALS_UPDATE_TO_DEVICE_ACTOR_MSG:
                processor.processCredentialsUpdate(msg);
                break;
            case DEVICE_NAME_OR_TYPE_UPDATE_TO_DEVICE_ACTOR_MSG:
                processor.processNameOrTypeUpdate((DeviceNameOrTypeUpdateMsg) msg);
                break;
            case DEVICE_RPC_REQUEST_TO_DEVICE_ACTOR_MSG:
                processor.processRpcRequest(ctx, (ToDeviceRpcRequestActorMsg) msg);
                break;
            case DEVICE_RPC_RESPONSE_TO_DEVICE_ACTOR_MSG:
                processor.processRpcResponsesFromEdge((FromDeviceRpcResponseActorMsg) msg);
                break;
            case DEVICE_ACTOR_SERVER_SIDE_RPC_TIMEOUT_MSG:
                processor.processServerSideRpcTimeout((DeviceActorServerSideRpcTimeoutMsg) msg);
                break;
            case SESSION_TIMEOUT_MSG:
                processor.checkSessionsTimeout();
                break;
            case DEVICE_EDGE_UPDATE_TO_DEVICE_ACTOR_MSG:
                processor.processEdgeUpdate((DeviceEdgeUpdateMsg) msg);
                break;
            case REMOVE_RPC_TO_DEVICE_ACTOR_MSG:
                processor.processRemoveRpc((RemoveRpcActorMsg) msg);
                break;
            default:
                return false;
        }
        return true;
    }

}
