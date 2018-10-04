/**
 * Copyright © 2016-2018 The Thingsboard Authors
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

import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.thingsboard.rule.engine.api.msg.DeviceAttributesEventNotificationMsg;
import org.thingsboard.rule.engine.api.msg.DeviceNameOrTypeUpdateMsg;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.service.ContextAwareActor;
import org.thingsboard.server.actors.service.ContextBasedCreator;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.cluster.ClusterEventMsg;
import org.thingsboard.server.common.msg.device.DeviceToDeviceActorMsg;
import org.thingsboard.server.common.msg.timeout.DeviceActorClientSideRpcTimeoutMsg;
import org.thingsboard.server.common.msg.timeout.DeviceActorQueueTimeoutMsg;
import org.thingsboard.server.common.msg.timeout.DeviceActorServerSideRpcTimeoutMsg;
import org.thingsboard.server.service.rpc.ToDeviceRpcRequestActorMsg;
import org.thingsboard.server.service.rpc.ToServerRpcResponseActorMsg;

public class DeviceActor extends ContextAwareActor {

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    private final DeviceActorMessageProcessor processor;

    private DeviceActor(ActorSystemContext systemContext, TenantId tenantId, DeviceId deviceId) {
        super(systemContext);
        this.processor = new DeviceActorMessageProcessor(systemContext, logger, tenantId, deviceId);
    }

    @Override
    protected boolean process(TbActorMsg msg) {
        switch (msg.getMsgType()) {
            case CLUSTER_EVENT_MSG:
                processor.processClusterEventMsg((ClusterEventMsg) msg);
                break;
            case DEVICE_SESSION_TO_DEVICE_ACTOR_MSG:
                processor.process(context(), (DeviceToDeviceActorMsg) msg);
                break;
            case DEVICE_ATTRIBUTES_UPDATE_TO_DEVICE_ACTOR_MSG:
                processor.processAttributesUpdate(context(), (DeviceAttributesEventNotificationMsg) msg);
                break;
            case DEVICE_CREDENTIALS_UPDATE_TO_DEVICE_ACTOR_MSG:
                processor.processCredentialsUpdate();
                break;
            case DEVICE_NAME_OR_TYPE_UPDATE_TO_DEVICE_ACTOR_MSG:
                processor.processNameOrTypeUpdate((DeviceNameOrTypeUpdateMsg) msg);
                break;
            case DEVICE_RPC_REQUEST_TO_DEVICE_ACTOR_MSG:
                processor.processRpcRequest(context(), (ToDeviceRpcRequestActorMsg) msg);
                break;
            case SERVER_RPC_RESPONSE_TO_DEVICE_ACTOR_MSG:
                processor.processToServerRPCResponse(context(), (ToServerRpcResponseActorMsg) msg);
                break;
            case DEVICE_ACTOR_SERVER_SIDE_RPC_TIMEOUT_MSG:
                processor.processServerSideRpcTimeout(context(), (DeviceActorServerSideRpcTimeoutMsg) msg);
                break;
            case DEVICE_ACTOR_CLIENT_SIDE_RPC_TIMEOUT_MSG:
                processor.processClientSideRpcTimeout(context(), (DeviceActorClientSideRpcTimeoutMsg) msg);
                break;
            case DEVICE_ACTOR_QUEUE_TIMEOUT_MSG:
                processor.processQueueTimeout(context(), (DeviceActorQueueTimeoutMsg) msg);
                break;
            case RULE_ENGINE_QUEUE_PUT_ACK_MSG:
                processor.processQueueAck(context(), (RuleEngineQueuePutAckMsg) msg);
                break;
            default:
                return false;
        }
        return true;
    }

    public static class ActorCreator extends ContextBasedCreator<DeviceActor> {
        private static final long serialVersionUID = 1L;

        private final TenantId tenantId;
        private final DeviceId deviceId;

        public ActorCreator(ActorSystemContext context, TenantId tenantId, DeviceId deviceId) {
            super(context);
            this.tenantId = tenantId;
            this.deviceId = deviceId;
        }

        @Override
        public DeviceActor create() throws Exception {
            return new DeviceActor(context, tenantId, deviceId);
        }
    }

}
